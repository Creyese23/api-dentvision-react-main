package co.edu.sena.productsreact.service;

// Importaciones de DTOs para facturas
import co.edu.sena.productsreact.dto.factura.DetalleFacturaRequest; // DTO con los datos de un ítem a agregar a la factura
import co.edu.sena.productsreact.dto.factura.FacturaRequest;        // DTO con los datos para crear una factura manualmente
import co.edu.sena.productsreact.dto.factura.FacturaResponse;       // DTO con los datos de la factura que se devuelven al cliente

// Importación comodín de todas las entidades JPA del paquete
import co.edu.sena.productsreact.entity.*;

// Excepción cuando no se encuentra el recurso solicitado
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Importación comodín de todos los repositorios del paquete
import co.edu.sena.productsreact.repository.*;

import lombok.RequiredArgsConstructor;        // Genera el constructor con inyección de dependencias
import lombok.extern.slf4j.Slf4j;            // Genera el objeto "log" para registrar advertencias
import org.springframework.stereotype.Service; // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones de escritura

import java.math.BigDecimal;             // Para operaciones precisas con valores monetarios
import java.text.NumberFormat;           // Para formatear el total de la factura en el email
import java.time.LocalDateTime;          // Para registrar la fecha de emisión y eliminación de facturas
import java.util.List;                   // Colección de elementos
import java.util.Locale;                 // Para usar el formato numérico colombiano en el email

// @Service: Spring registra esta clase como componente de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok inyecta todos los repositorios por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): las consultas son de solo lectura por defecto
@org.springframework.transaction.annotation.Transactional(readOnly = true)
// @Slf4j: Lombok genera "log" para usar log.warn() y log.info()
@Slf4j
public class FacturaService {

    // Repositorio para crear y consultar facturas en la BD
    private final FacturaRepository facturaRepository;
    // Repositorio para agregar y consultar los ítems (líneas) de cada factura
    private final DetalleFacturaRepository detalleRepository;
    // Repositorio para buscar el paciente al que se emite la factura
    private final PacienteRepository pacienteRepository;
    // Repositorio para buscar el odontólogo que realizó el tratamiento
    private final EmpleadoRepository empleadoRepository;
    // Repositorio para buscar el servicio odontológico en los detalles de factura
    private final ServicioRepository servicioRepository;
    // Repositorio para buscar el sub-servicio y obtener su precio específico
    private final SubServicioRepository subServicioRepository;
    // Repositorio para buscar la cita cuando se genera la factura automáticamente
    private final CitaRepository citaRepository;
    // Repositorio para obtener los procedimientos completados de una cita (para generar la factura automática)
    private final ProcedimientoRepository procedimientoRepository;
    // Servicio para enviar el email de notificación de factura al paciente
    private final EmailService emailService;

    /**
     * Lista todas las facturas de un paciente específico que no han sido eliminadas.
     * El paciente puede consultar su historial de facturas desde el portal.
     */
    public List<FacturaResponse> listarPorPaciente(Long pacienteId) {
        // Busca facturas del paciente que no tienen fecha de eliminación (no borradas lógicamente)
        return facturaRepository.findByPacienteIdAndFechaEliminacionIsNull(pacienteId)
                .stream().map(FacturaResponse::from).toList();
    }

    /**
     * Obtiene una factura específica por su ID con todos sus detalles.
     * Lanza excepción si la factura no existe.
     */
    public FacturaResponse obtener(Long id) {
        return FacturaResponse.from(getOrThrow(id)); // getOrThrow valida existencia y lanza excepción si no existe
    }

    @Transactional // Esta operación crea una factura vacía en la BD
    /**
     * Crea una factura vacía manualmente (sin detalles ni total).
     * Los detalles se agregan después con el método agregarDetalle.
     * El odontólogo es opcional (puede no estar asignado al momento de crear la factura).
     */
    public FacturaResponse crear(FacturaRequest req) {
        // Busca el paciente al que se emitirá la factura
        Paciente paciente = pacienteRepository.findById(req.idPaciente())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + req.idPaciente() + " no encontrado"));
        // El odontólogo es opcional en el request; si viene null, la factura se crea sin odontólogo asignado
        Empleado odontologo = null;
        if (req.idOdontologo() != null) {
            odontologo = empleadoRepository.findById(req.idOdontologo())
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado " + req.idOdontologo() + " no encontrado"));
        }
        // Construye la factura con total cero; los detalles se agregarán posteriormente
        Factura factura = Factura.builder()
                .paciente(paciente)                  // Paciente que recibirá la factura
                .odontologo(odontologo)              // Odontólogo tratante (puede ser null)
                .fechaEmision(LocalDateTime.now())   // Fecha y hora de emisión de la factura
                .total(BigDecimal.ZERO)              // Total inicial en cero; se recalculará al agregar detalles
                .estado("PENDIENTE")                 // Estado inicial: pendiente de pago
                .build();
        return FacturaResponse.from(facturaRepository.save(factura)); // Guarda y retorna el DTO
    }

    @Transactional // Esta operación agrega un ítem a la factura y recalcula el total
    /**
     * Agrega un ítem (línea de detalle) a una factura existente.
     * Si se especifica un sub-servicio, usa su precio; si no, el precio es cero.
     * Recalcula automáticamente el total de la factura al agregar el detalle.
     */
    public FacturaResponse agregarDetalle(Long facturaId, DetalleFacturaRequest req) {
        Factura factura = getOrThrow(facturaId); // Obtiene la factura a la que se agrega el detalle
        // Busca el servicio padre del ítem (ej. "Ortodoncia", "Limpieza")
        Servicio servicio = servicioRepository.findById(req.idServicio())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio " + req.idServicio() + " no encontrado"));

        BigDecimal precioUnitario = BigDecimal.ZERO; // Precio inicial en cero
        if (req.idSubServicio() != null) {
            // Si se especificó un sub-servicio, obtiene su precio específico
            SubServicio sub = subServicioRepository.findById(req.idSubServicio())
                    .orElseThrow(() -> new ResourceNotFoundException("SubServicio " + req.idSubServicio() + " no encontrado"));
            if (sub.getPrecio() != null) {
                precioUnitario = sub.getPrecio(); // Usa el precio del sub-servicio si está definido
            }
        }

        // Calcula el subtotal multiplicando el precio unitario por la cantidad solicitada
        BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(req.cantidad()));

        // Construye y guarda el detalle de la factura (una línea del ítem)
        DetalleFactura detalle = DetalleFactura.builder()
                .factura(factura)                    // Factura a la que pertenece este detalle
                .servicio(servicio)                  // Servicio odontológico cobrado
                .cantidad(req.cantidad())            // Cantidad de veces que se realizó el servicio
                .precioUnitario(precioUnitario)      // Precio por unidad del servicio
                .subtotal(subtotal)                  // Precio unitario × cantidad
                .build();
        detalleRepository.save(detalle); // Persiste el nuevo detalle en la BD

        recalcularTotal(factura); // Suma todos los subtotales de los detalles y actualiza el total de la factura
        return FacturaResponse.from(facturaRepository.save(factura)); // Guarda el total actualizado y retorna el DTO
    }

    @Transactional // Esta operación actualiza el estado de la factura en la BD
    /**
     * Cambia el estado de una factura (PENDIENTE → PAGADA, CANCELADA, etc.).
     * Se usa cuando el pago se registra manualmente o cuando se cancela la factura.
     */
    public FacturaResponse cambiarEstado(Long id, String estado) {
        Factura f = getOrThrow(id); // Obtiene la factura
        f.setEstado(estado);        // Actualiza el estado con el nuevo valor recibido
        return FacturaResponse.from(facturaRepository.save(f)); // Persiste y retorna el DTO
    }

    @Transactional // Esta operación puede crear múltiples registros en la BD (factura + detalles)
    /**
     * Genera automáticamente una factura completa a partir de los procedimientos completados de una cita.
     * Este método es invocado por ProcedimientoService cuando TODOS los procedimientos de una cita quedan COMPLETADOS.
     * Calcula el total sumando los costos de cada procedimiento y crea un detalle por cada uno.
     */
    public FacturaResponse crearDesdeCita(Long citaId) {
        // Busca la cita para obtener el paciente y odontólogo asociados
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita " + citaId + " no encontrada"));

        // Obtiene solo los procedimientos que están en estado COMPLETADO y no eliminados
        List<Procedimiento> completados = procedimientoRepository
                .findByCitaIdAndFechaEliminacionIsNull(citaId)
                .stream()
                .filter(p -> "COMPLETADO".equals(p.getEstado())) // Filtra solo los completados
                .toList();

        // Calcula el total de la factura sumando el costo de cada procedimiento completado
        // Si un procedimiento no tiene costo definido (null), se omite en la suma
        BigDecimal total = completados.stream()
                .filter(p -> p.getCosto() != null)         // Excluye procedimientos sin costo
                .map(Procedimiento::getCosto)              // Obtiene el costo de cada procedimiento
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Suma todos los costos; si la lista está vacía, retorna 0

        // Construye la factura automática vinculada a la cita
        Factura factura = Factura.builder()
                .paciente(cita.getPaciente())          // El paciente es el de la cita
                .odontologo(cita.getOdontologo())      // El odontólogo es el que atendió la cita
                .fechaEmision(LocalDateTime.now())     // Fecha y hora de generación automática
                .total(total)                          // Total calculado de los procedimientos completados
                .estado("PENDIENTE")                   // La factura queda pendiente de pago
                .build();
        factura = facturaRepository.save(factura); // Persiste la factura en la BD y obtiene su ID

        // Crear un DetalleFactura por cada procedimiento completado
        // Busca o crea el servicio genérico "Odontología General" para categorizar los detalles
        Servicio servicioGeneral = servicioRepository.findFirstByNombre("Odontología General")
                .orElseGet(() -> servicioRepository.save(Servicio.builder()
                        .nombre("Odontología General")
                        .descripcion("Procedimientos odontológicos generales")
                        .estado(true).build())); // Si no existe, lo crea para poder usarlo como categoría

        final Factura facturaFinal = factura; // Variable final necesaria para usar dentro del lambda
        // Itera sobre los procedimientos completados que tienen costo definido
        completados.stream().filter(p -> p.getCosto() != null).forEach(p -> {
            BigDecimal costo = p.getCosto(); // Costo del procedimiento para este ítem de detalle
            // Crea un ítem de detalle por cada procedimiento con su nombre y costo
            detalleRepository.save(DetalleFactura.builder()
                    .factura(facturaFinal)           // Factura a la que pertenece este detalle
                    .servicio(servicioGeneral)       // Categoría del servicio (Odontología General)
                    .concepto(p.getNombre())         // Nombre del procedimiento como concepto del ítem
                    .cantidad(1)                     // Cada procedimiento cuenta como 1 unidad
                    .precioUnitario(costo)           // Precio unitario igual al costo del procedimiento
                    .subtotal(costo)                 // Subtotal igual al costo (cantidad=1 × precio)
                    .build());
        });

        FacturaResponse resp = FacturaResponse.from(facturaFinal); // Convierte la factura a DTO
        // Intenta enviar email de notificación al paciente informándole que tiene una factura lista
        try {
            String email = cita.getPaciente().getUsuario().getEmail();     // Email del paciente
            String nombre = cita.getPaciente().getUsuario().getPerfil().getNombres(); // Nombre del paciente
            // Formatea el total en formato colombiano (ej. "1.500.000") para el email
            String totalStr = NumberFormat.getNumberInstance(new Locale("es", "CO"))
                .format(total);
            // Envía el email con el ID de la factura y el total a pagar
            emailService.sendFacturaGenerada(email, nombre, resp.id(), totalStr);
        } catch (Exception e) {
            // Si el email falla, no interrumpe la generación de la factura
            log.warn("No se pudo enviar email de factura: {}", e.getMessage());
        }
        return resp; // Retorna el DTO de la factura recién creada
    }

    @Transactional // Esta operación hace eliminación lógica de la factura
    /**
     * Elimina lógicamente una factura registrando la fecha de eliminación.
     * La factura no se borra físicamente para mantener el historial contable.
     */
    public void eliminar(Long id) {
        Factura f = getOrThrow(id);                       // Obtiene la factura
        f.setFechaEliminacion(LocalDateTime.now());       // Registra la fecha y hora de eliminación lógica
        facturaRepository.save(f);                        // Persiste el cambio en la BD
    }

    /**
     * Método privado auxiliar: recalcula el total de una factura sumando todos sus detalles.
     * Se llama cada vez que se agrega un nuevo ítem a la factura para mantener el total actualizado.
     */
    private void recalcularTotal(Factura factura) {
        // Busca todos los ítems de la factura en la BD
        BigDecimal total = detalleRepository.findByFacturaId(factura.getId())
                .stream().map(DetalleFactura::getSubtotal)              // Obtiene el subtotal de cada ítem
                .reduce(BigDecimal.ZERO, BigDecimal::add);              // Suma todos los subtotales
        factura.setTotal(total); // Actualiza el total de la factura con la suma calculada
    }

    /**
     * Método auxiliar privado: busca una factura por ID y lanza excepción si no existe.
     * Se reutiliza en todos los métodos para evitar código duplicado.
     */
    private Factura getOrThrow(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura " + id + " no encontrada"));
    }
}

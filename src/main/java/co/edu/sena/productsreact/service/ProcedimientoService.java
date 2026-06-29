package co.edu.sena.productsreact.service;

// Importaciones de DTOs para procedimientos e insumos
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoInsumoResponse; // DTO que representa el uso de un insumo en un procedimiento
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoRequest;        // DTO con datos de entrada para crear o actualizar un procedimiento
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoResponse;       // DTO con los datos del procedimiento que se devuelven al cliente

// Importación comodín de todas las entidades JPA del paquete
import co.edu.sena.productsreact.entity.*;

// Excepción cuando no se encuentra un recurso en la BD
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Importación comodín de todos los repositorios del paquete
import co.edu.sena.productsreact.repository.*;

import lombok.RequiredArgsConstructor;        // Genera el constructor con inyección de dependencias
import lombok.extern.slf4j.Slf4j;            // Genera el objeto "log" para registrar mensajes en el servidor
import org.springframework.context.annotation.Lazy; // Para romper dependencias circulares (FacturaService <-> ProcedimientoService)
import org.springframework.stereotype.Service;      // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones de escritura

import java.time.LocalDateTime; // Para registrar fechas de finalización y eliminación
import java.util.List;          // Colección de elementos

// @Service: Spring registra esta clase como componente de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok inyecta todos los repositorios y servicios por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): las consultas de solo lectura son el comportamiento por defecto
@org.springframework.transaction.annotation.Transactional(readOnly = true)
// @Slf4j: Lombok genera "log" para usar log.warn(), log.info(), etc.
@Slf4j
public class ProcedimientoService {

    // Repositorio para consultar y persistir procedimientos odontológicos en la BD
    private final ProcedimientoRepository procedimientoRepository;
    // Repositorio para buscar la cita a la que se asocia el procedimiento
    private final CitaRepository citaRepository;
    // Repositorio para buscar al técnico asignado al procedimiento
    private final EmpleadoRepository empleadoRepository;
    // Repositorio para buscar el usuario por email (para identificar al paciente o técnico autenticado)
    private final UserRepository userRepository;
    // Repositorio para buscar el paciente vinculado al usuario autenticado
    private final PacienteRepository pacienteRepository;
    // Repositorio para buscar y actualizar el stock de insumos utilizados en el procedimiento
    private final InsumoRepository insumoRepository;
    // Repositorio para registrar movimientos de inventario (entradas y salidas de insumos)
    private final MovimientoInventarioRepository movimientoRepository;
    // Repositorio para gestionar qué insumos se usaron en cada procedimiento y en qué cantidad
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
    // Servicio para enviar notificaciones por email al paciente sobre cambios en su procedimiento
    private final EmailService emailService;
    // @Lazy rompe la dependencia circular: FacturaService depende de ProcedimientoService
    // y ProcedimientoService depende de FacturaService; @Lazy retrasa la inicialización de uno de ellos
    @Lazy private final FacturaService facturaService;

    /**
     * Lista el historial de procedimientos del paciente autenticado.
     * El paciente puede ver qué tratamientos ha recibido y cuál es su estado.
     */
    public List<ProcedimientoResponse> listarMiHistorial(String email) {
        // Busca el usuario por su email del token JWT
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        // Busca el perfil de paciente vinculado al usuario
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        // Busca todos los procedimientos de las citas del paciente con sus detalles (cita, técnico, etc.)
        return procedimientoRepository.findByCitaPacienteIdWithDetails(paciente.getId())
                .stream().map(ProcedimientoResponse::from).toList();
    }

    /**
     * Lista los procedimientos asignados al técnico actualmente autenticado.
     * Los técnicos dentales ven solo los trabajos que tienen asignados.
     */
    public List<ProcedimientoResponse> listarPorTecnicoAutenticado(String email) {
        // Busca el usuario autenticado por email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        // Busca el registro de empleado del usuario; si no tiene registro de empleado, retorna lista vacía
        return empleadoRepository.findByUsuarioId(user.getId())
                .map(tecnico -> procedimientoRepository.findByTecnicoIdWithDetails(tecnico.getId())
                        .stream().map(ProcedimientoResponse::from).toList())
                .orElse(java.util.List.of()); // Si no es empleado, retorna lista vacía en lugar de error
    }

    /**
     * Lista todos los procedimientos asociados a una cita específica.
     * Una cita puede tener múltiples procedimientos (ej. limpieza + radiografía).
     */
    public List<ProcedimientoResponse> listarPorCita(Long citaId) {
        // Busca procedimientos de la cita con sus datos de detalle (técnico, cita, etc.)
        return procedimientoRepository.findByCitaIdWithDetails(citaId)
                .stream().map(ProcedimientoResponse::from).toList();
    }

    /**
     * Obtiene un procedimiento específico por su ID con todos sus detalles.
     * Lanza excepción si no existe.
     */
    public ProcedimientoResponse obtener(Long id) {
        // Usa findByIdWithDetails que hace JOIN con la cita y el técnico para evitar lazy-loading
        Procedimiento p = procedimientoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + id + " no encontrado"));
        return ProcedimientoResponse.from(p);
    }

    @Transactional // Esta operación crea un procedimiento en la BD
    /**
     * Crea un nuevo procedimiento odontológico vinculado a una cita.
     * El técnico es opcional (puede asignarse después).
     */
    public ProcedimientoResponse crear(ProcedimientoRequest req) {
        // Busca la cita a la que se asociará el procedimiento
        Cita cita = citaRepository.findById(req.idCita())
                .orElseThrow(() -> new ResourceNotFoundException("Cita " + req.idCita() + " no encontrada"));

        // El técnico es opcional; si no se especifica, el procedimiento queda sin técnico asignado
        Empleado tecnico = null;
        if (req.idTecnico() != null) {
            // Si se envió un ID de técnico, lo busca en la BD
            tecnico = empleadoRepository.findById(req.idTecnico())
                    .orElseThrow(() -> new ResourceNotFoundException("Técnico " + req.idTecnico() + " no encontrado"));
        }

        // Construye la entidad Procedimiento con todos los datos del request
        Procedimiento p = Procedimiento.builder()
                .cita(cita)                          // Cita odontológica asociada
                .tecnico(tecnico)                    // Técnico asignado (puede ser null)
                .nombre(req.nombre())                // Nombre del procedimiento (ej. "Extracción de molar")
                .descripcion(req.descripcion())      // Descripción detallada del procedimiento
                .costo(req.costo())                  // Costo del procedimiento (se usará para calcular la factura)
                .estado("EN_PROCESO")                // Estado inicial: el procedimiento está en proceso
                .fechaInicio(req.fechaInicio())      // Fecha en que inicia el procedimiento
                .fechaFin(req.fechaFin())            // Fecha prevista de finalización (puede ser null)
                .observaciones(req.observaciones())  // Observaciones clínicas del odontólogo
                .build();
        return ProcedimientoResponse.from(procedimientoRepository.save(p));
    }

    @Transactional // Esta operación actualiza el procedimiento en la BD
    /**
     * Actualiza los datos de un procedimiento existente (técnico, descripción, costo, fechas, observaciones).
     */
    public ProcedimientoResponse actualizar(Long id, ProcedimientoRequest req) {
        Procedimiento p = getOrThrow(id); // Obtiene el procedimiento existente
        // El técnico puede cambiar o asignarse si inicialmente era null
        Empleado tecnico = null;
        if (req.idTecnico() != null) {
            tecnico = empleadoRepository.findById(req.idTecnico())
                    .orElseThrow(() -> new ResourceNotFoundException("Técnico " + req.idTecnico() + " no encontrado"));
        }
        p.setTecnico(tecnico);                        // Actualiza el técnico asignado
        p.setNombre(req.nombre());                    // Actualiza el nombre del procedimiento
        p.setDescripcion(req.descripcion());          // Actualiza la descripción
        p.setCosto(req.costo());                      // Actualiza el costo (afectará la factura)
        p.setFechaInicio(req.fechaInicio());          // Actualiza la fecha de inicio
        p.setFechaFin(req.fechaFin());                // Actualiza la fecha de fin
        p.setObservaciones(req.observaciones());      // Actualiza las observaciones clínicas
        return ProcedimientoResponse.from(procedimientoRepository.save(p));
    }

    @Transactional // Esta operación puede actualizar el procedimiento, enviar email y crear factura
    /**
     * Cambia el estado de un procedimiento (EN_PROCESO → COMPLETADO, etc.).
     * Si se completa, registra la fecha de fin, notifica al paciente por email
     * y verifica si todos los procedimientos de la cita están completos para auto-generar la factura.
     */
    public ProcedimientoResponse cambiarEstado(Long id, String estado) {
        Procedimiento p = getOrThrow(id); // Obtiene el procedimiento
        p.setEstado(estado);              // Actualiza el estado al nuevo valor
        // Si el procedimiento se completa y no tenía fecha de fin, se registra la fecha actual
        if ("COMPLETADO".equals(estado) && p.getFechaFin() == null) {
            p.setFechaFin(LocalDateTime.now()); // Registra automáticamente cuándo terminó el procedimiento
        }
        ProcedimientoResponse resultado = ProcedimientoResponse.from(procedimientoRepository.save(p));

        // Notificar al paciente sobre el cambio de estado
        try {
            // Obtiene el email del paciente a través de la cadena: procedimiento → cita → paciente → usuario → email
            String email = p.getCita().getPaciente().getUsuario().getEmail();
            String nombre = p.getCita().getPaciente().getUsuario().getPerfil().getNombres();
            // Envía email informando al paciente el nuevo estado de su procedimiento
            emailService.sendProcedimientoActualizado(email, nombre, p.getNombre(), estado);
        } catch (Exception e) {
            // Si el email falla, no se interrumpe el cambio de estado
            log.warn("No se pudo enviar email de procedimiento: {}", e.getMessage());
        }

        // Auto-generar factura cuando todos los procedimientos de la cita están COMPLETADO
        if ("COMPLETADO".equals(estado)) {
            Long citaId = p.getCita().getId(); // Obtiene el ID de la cita a la que pertenece este procedimiento
            // Busca todos los procedimientos no eliminados de esa cita
            List<Procedimiento> todos = procedimientoRepository.findByCitaIdAndFechaEliminacionIsNull(citaId);
            // Verifica que la lista no esté vacía Y que todos los procedimientos estén COMPLETADOS
            boolean todosCompletos = !todos.isEmpty() && todos.stream().allMatch(pr -> "COMPLETADO".equals(pr.getEstado()));
            if (todosCompletos) {
                // Si todos los procedimientos de la cita están completos, genera automáticamente la factura
                facturaService.crearDesdeCita(citaId);
            }
        }

        return resultado; // Retorna el DTO del procedimiento con el estado actualizado
    }

    @Transactional // Esta operación hace eliminación lógica del procedimiento
    /**
     * Elimina lógicamente un procedimiento registrando la fecha de eliminación.
     * No borra el registro de la BD para mantener el historial clínico.
     */
    public void eliminar(Long id) {
        Procedimiento p = getOrThrow(id);                    // Obtiene el procedimiento
        p.setFechaEliminacion(LocalDateTime.now());          // Registra la fecha de eliminación lógica
        procedimientoRepository.save(p);                     // Persiste el cambio en la BD
    }

    // ── Insumos por procedimiento ─────────────────────────────────────────────

    /**
     * Lista todos los insumos utilizados en un procedimiento específico.
     * Permite ver qué materiales (guantes, resina, etc.) se consumieron en el tratamiento.
     */
    public List<ProcedimientoInsumoResponse> listarInsumos(Long procedimientoId) {
        getOrThrow(procedimientoId); // valida existencia del procedimiento antes de listar sus insumos
        // Busca los registros de insumos asociados al procedimiento y los convierte a DTOs
        return procedimientoInsumoRepository.findByProcedimientoId(procedimientoId)
                .stream().map(ProcedimientoInsumoResponse::from).toList();
    }

    @Transactional // Esta operación actualiza el stock del insumo, registra el movimiento y crea el vínculo
    /**
     * Registra el uso de un insumo en un procedimiento.
     * Verifica que haya stock suficiente, descuenta del inventario y registra el movimiento de salida.
     */
    public ProcedimientoInsumoResponse registrarInsumo(Long procedimientoId, Long insumoId, Integer cantidad) {
        Procedimiento procedimiento = getOrThrow(procedimientoId); // Verifica que el procedimiento exista
        // Busca el insumo que se utilizó en el procedimiento
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo " + insumoId + " no encontrado"));

        // Valida que el stock disponible sea suficiente para la cantidad a consumir
        if (insumo.getStockActual() < cantidad) {
            // Lanza excepción informando el stock disponible vs. el solicitado
            throw new IllegalArgumentException("Stock insuficiente. Disponible: " + insumo.getStockActual()
                    + " " + (insumo.getUnidadMedida() != null ? insumo.getUnidadMedida() : ""));
        }

        // Descuenta la cantidad utilizada del stock actual del insumo
        insumo.setStockActual(insumo.getStockActual() - cantidad);
        insumoRepository.save(insumo); // Persiste el nuevo stock en la BD

        // Registra el movimiento de SALIDA en el historial de inventario para trazabilidad
        movimientoRepository.save(MovimientoInventario.builder()
                .insumo(insumo).cantidad(cantidad).tipoMovimiento("SALIDA")
                .build());

        // Crea el registro que vincula el insumo al procedimiento con la cantidad usada
        ProcedimientoInsumo pi = ProcedimientoInsumo.builder()
                .procedimiento(procedimiento)  // Procedimiento que consumió el insumo
                .insumo(insumo)                // Insumo que fue consumido
                .cantidad(cantidad)            // Cantidad exacta consumida
                .build();
        return ProcedimientoInsumoResponse.from(procedimientoInsumoRepository.save(pi));
    }

    @Transactional // Esta operación devuelve stock, registra movimiento y elimina el vínculo
    /**
     * Elimina el registro de uso de un insumo en un procedimiento.
     * Devuelve al inventario la cantidad que se había descontado anteriormente.
     */
    public void eliminarInsumo(Long procedimientoInsumoId) {
        // Busca el registro ProcedimientoInsumo que se desea eliminar
        ProcedimientoInsumo pi = procedimientoInsumoRepository.findById(procedimientoInsumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de insumo no encontrado"));

        Insumo insumo = pi.getInsumo(); // Obtiene el insumo para devolverle el stock
        // Devuelve al stock la cantidad que se había descontado cuando se registró el uso
        insumo.setStockActual(insumo.getStockActual() + pi.getCantidad());
        insumoRepository.save(insumo); // Persiste el stock actualizado en la BD

        // Registra el movimiento de ENTRADA (devolución al inventario) para mantener la trazabilidad
        movimientoRepository.save(MovimientoInventario.builder()
                .insumo(insumo).cantidad(pi.getCantidad()).tipoMovimiento("ENTRADA")
                .build());

        // Elimina físicamente el registro de uso del insumo en el procedimiento
        procedimientoInsumoRepository.delete(pi);
    }

    /**
     * Método auxiliar público: busca un procedimiento por ID y lanza excepción si no existe.
     * Es público porque se reutiliza en otros servicios como OrdenService.
     */
    public Procedimiento getOrThrow(Long id) {
        return procedimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + id + " no encontrado"));
    }
}

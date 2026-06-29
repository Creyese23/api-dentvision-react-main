package co.edu.sena.productsreact.service;

// Importaciones de DTOs para insumos y movimientos de inventario
import co.edu.sena.productsreact.dto.insumo.InsumoRequest;      // DTO con los datos para crear o actualizar un insumo
import co.edu.sena.productsreact.dto.insumo.InsumoResponse;     // DTO con los datos del insumo que se devuelven al cliente
import co.edu.sena.productsreact.dto.insumo.MovimientoRequest;  // DTO con los datos para registrar una entrada o salida de inventario

// Entidades JPA que representan tablas en la BD
import co.edu.sena.productsreact.entity.Empleado;              // Entidad del empleado responsable del movimiento de inventario
import co.edu.sena.productsreact.entity.Insumo;                // Entidad del insumo (material dental como guantes, resina, etc.)
import co.edu.sena.productsreact.entity.MovimientoInventario;  // Entidad que registra cada entrada/salida del inventario

// Excepción cuando no se encuentra el recurso solicitado
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Repositorios para acceder a las tablas en la BD
import co.edu.sena.productsreact.repository.EmpleadoRepository;              // Repositorio para buscar el empleado responsable del movimiento
import co.edu.sena.productsreact.repository.InsumoRepository;                // Repositorio para gestionar insumos
import co.edu.sena.productsreact.repository.MovimientoInventarioRepository;  // Repositorio para registrar historial de movimientos

import lombok.RequiredArgsConstructor; // Genera el constructor con inyección de dependencias
import org.springframework.stereotype.Service;                    // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones de escritura

import java.time.LocalDateTime; // Para registrar la fecha de eliminación lógica
import java.util.List;          // Colección de elementos

// @Service: Spring registra esta clase como componente de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok inyecta los tres repositorios por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): las consultas son de solo lectura por defecto
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class InsumoService {

    // Repositorio para consultar y persistir insumos en la BD
    private final InsumoRepository insumoRepository;
    // Repositorio para guardar el historial de entradas y salidas del inventario
    private final MovimientoInventarioRepository movimientoRepository;
    // Repositorio para buscar al empleado que realizó el movimiento de inventario (opcional)
    private final EmpleadoRepository empleadoRepository;

    /**
     * Lista todos los insumos activos y no eliminados del inventario.
     * Se usa para mostrar el catálogo completo de materiales disponibles.
     */
    public List<InsumoResponse> listarActivos() {
        // Busca insumos con estado=true y sin fecha de eliminación, convierte cada uno a DTO
        return insumoRepository.findByEstadoTrueAndFechaEliminacionIsNull()
                .stream().map(InsumoResponse::from).toList();
    }

    /**
     * Lista los insumos cuyo stock actual está por debajo del mínimo establecido.
     * Permite al administrador identificar qué materiales necesitan reabastecimiento urgente.
     */
    public List<InsumoResponse> listarBajoStock() {
        // Consulta personalizada en el repositorio que filtra insumos con stockActual < stockMinimo
        return insumoRepository.findBajoStock().stream().map(InsumoResponse::from).toList();
    }

    /**
     * Obtiene un insumo específico por su ID.
     * Lanza excepción si no existe.
     */
    public InsumoResponse obtener(Long id) {
        return InsumoResponse.from(getOrThrow(id)); // getOrThrow lanza excepción si no encuentra el insumo
    }

    @Transactional // Esta operación crea un nuevo registro de insumo en la BD
    /**
     * Crea un nuevo insumo en el inventario.
     * Si no se especifica stock actual o mínimo, se inicializan en 0.
     */
    public InsumoResponse crear(InsumoRequest req) {
        // Construye la entidad Insumo con los datos del request
        Insumo i = Insumo.builder()
                .nombre(req.nombre())             // Nombre del insumo (ej. "Guantes de látex talla M")
                .descripcion(req.descripcion())   // Descripción detallada del insumo
                .unidadMedida(req.unidadMedida()) // Unidad de medida (ej. "par", "ml", "unidad")
                // Si no se envía stock actual, se inicia en 0 para evitar null en la BD
                .stockActual(req.stockActual() != null ? req.stockActual() : 0)
                // Si no se envía stock mínimo, se inicia en 0 (sin umbral de alerta)
                .stockMinimo(req.stockMinimo() != null ? req.stockMinimo() : 0)
                .precioUnitario(req.precioUnitario()) // Precio por unidad del insumo
                .estado(true)                     // El insumo queda activo al crearse
                .build();
        return InsumoResponse.from(insumoRepository.save(i)); // Persiste y retorna el DTO
    }

    @Transactional // Esta operación actualiza el insumo en la BD
    /**
     * Actualiza los datos de un insumo existente.
     * Stock actual y mínimo solo se actualizan si vienen con valor en el request.
     */
    public InsumoResponse actualizar(Long id, InsumoRequest req) {
        Insumo i = getOrThrow(id);                          // Obtiene el insumo existente
        i.setNombre(req.nombre());                          // Actualiza el nombre
        i.setDescripcion(req.descripcion());                // Actualiza la descripción
        i.setUnidadMedida(req.unidadMedida());             // Actualiza la unidad de medida
        // Solo actualiza el stock actual si se envió un valor (evita borrar el stock accidentalmente)
        if (req.stockActual() != null) i.setStockActual(req.stockActual());
        // Solo actualiza el stock mínimo si se envió un valor; si no, mantiene el actual
        i.setStockMinimo(req.stockMinimo() != null ? req.stockMinimo() : i.getStockMinimo());
        i.setPrecioUnitario(req.precioUnitario());         // Actualiza el precio unitario
        return InsumoResponse.from(insumoRepository.save(i)); // Guarda los cambios y retorna el DTO
    }

    @Transactional // Esta operación actualiza el stock y registra el movimiento en la BD
    /**
     * Registra una entrada o salida de inventario para un insumo.
     * ENTRADA: aumenta el stock (compra de material).
     * SALIDA: disminuye el stock (uso en procedimiento) — valida que haya suficiente stock.
     */
    public InsumoResponse registrarMovimiento(MovimientoRequest req) {
        Insumo insumo = getOrThrow(req.idInsumo()); // Obtiene el insumo al que se le registrará el movimiento
        // El empleado que realiza el movimiento es opcional
        Empleado empleado = null;
        if (req.idEmpleado() != null) {
            // Si se especificó un empleado, lo busca en la BD para registrar responsabilidad
            empleado = empleadoRepository.findById(req.idEmpleado())
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado " + req.idEmpleado() + " no encontrado"));
        }

        // Convierte el tipo de movimiento a mayúsculas para comparación uniforme
        String tipo = req.tipoMovimiento().toUpperCase();
        if ("ENTRADA".equals(tipo)) {
            // ENTRADA: se recibe mercancía, el stock aumenta con la cantidad recibida
            insumo.setStockActual(insumo.getStockActual() + req.cantidad());
        } else if ("SALIDA".equals(tipo)) {
            // SALIDA: se consume material; primero se verifica que haya suficiente stock disponible
            if (insumo.getStockActual() < req.cantidad()) {
                // Lanza excepción indicando cuántas unidades hay disponibles vs. cuántas se necesitan
                throw new IllegalArgumentException("Stock insuficiente. Disponible: " + insumo.getStockActual());
            }
            // Descuenta la cantidad consumida del stock actual
            insumo.setStockActual(insumo.getStockActual() - req.cantidad());
        }

        // Construye el registro de movimiento para el historial de inventario
        MovimientoInventario mov = MovimientoInventario.builder()
                .insumo(insumo)             // Insumo afectado por el movimiento
                .empleado(empleado)         // Empleado responsable (puede ser null)
                .cantidad(req.cantidad())   // Cantidad que entró o salió
                .tipoMovimiento(tipo)       // "ENTRADA" o "SALIDA"
                .build();
        movimientoRepository.save(mov); // Persiste el registro de movimiento en el historial
        return InsumoResponse.from(insumoRepository.save(insumo)); // Guarda el nuevo stock y retorna el DTO
    }

    @Transactional // Esta operación hace eliminación lógica del insumo
    /**
     * Elimina lógicamente un insumo del inventario.
     * El registro permanece en la BD para mantener la trazabilidad del historial.
     */
    public void eliminar(Long id) {
        Insumo i = getOrThrow(id);                    // Obtiene el insumo
        i.setEstado(false);                           // Marca el insumo como inactivo (no aparece en el inventario activo)
        i.setFechaEliminacion(LocalDateTime.now());   // Registra la fecha y hora de la eliminación lógica
        insumoRepository.save(i);                     // Persiste el cambio en la BD
    }

    /**
     * Método auxiliar privado: busca un insumo por ID y lanza excepción si no existe.
     * Se reutiliza en todos los métodos que necesitan verificar que el insumo exista.
     */
    private Insumo getOrThrow(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo " + id + " no encontrado"));
    }
}

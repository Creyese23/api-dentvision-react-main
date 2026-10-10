package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.orden.OrdenArchivoDiagnosticoRequest;
// Importaciones de DTOs para órdenes de trabajo
import co.edu.sena.productsreact.dto.orden.OrdenRequest;  // DTO con los datos para crear una nueva orden
import co.edu.sena.productsreact.dto.orden.OrdenResponse; // DTO con los datos de una orden que se devuelve al cliente

// Entidades JPA que representan tablas en la base de datos
import co.edu.sena.productsreact.entity.Entrega;       // Entidad de la entrega asociada a cada orden
import co.edu.sena.productsreact.entity.Notificacion;  // Entidad de notificación in-app
import co.edu.sena.productsreact.entity.Orden;         // Entidad de la orden de trabajo (trabajo a realizar al paciente)
import co.edu.sena.productsreact.entity.Procedimiento; // Entidad del procedimiento al que pertenece la orden

// Excepción cuando no se encuentra un recurso en la BD
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Repositorios para acceder a las tablas en la BD
import co.edu.sena.productsreact.repository.EntregaRepository;      // Repositorio para gestionar entregas
import co.edu.sena.productsreact.repository.NotificacionRepository; // Repositorio para guardar notificaciones in-app
import co.edu.sena.productsreact.repository.OrdenRepository;        // Repositorio para gestionar órdenes
import co.edu.sena.productsreact.repository.ProcedimientoRepository; // Repositorio para verificar el procedimiento padre
import co.edu.sena.productsreact.repository.UserRepository;         // Repositorio para buscar usuarios por rol

import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor; // Genera el constructor con inyección de dependencias
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;                    // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones de escritura
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.time.LocalDateTime; // Para registrar la fecha y hora de la entrega
import java.util.List;          // Colección de elementos
import java.util.Map;
import java.util.regex.Pattern;

// @Service: Spring registra esta clase como componente de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok inyecta los repositorios por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): las operaciones de consulta no modifican la BD
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@Slf4j
public class OrdenService {
    private static final String DIAGNOSTIC_MANIFEST_PREFIX =
            "{\"format\":\"dentvision-diagnostic-files-v1\",\"files\":[";
    private static final int MAX_DIAGNOSTIC_FILE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_DIAGNOSTIC_BASE64_CHARS = 14_000_000;
    private static final Pattern DIAGNOSTIC_FILE_NAME_PATTERN =
            Pattern.compile("[<>:\"/\\\\|?*\\u0000-\\u001f]");

    // Repositorio principal para consultar y persistir órdenes de trabajo en la BD
    private final OrdenRepository ordenRepository;
    // Repositorio para verificar que el procedimiento al que se vincula la orden exista
    private final ProcedimientoRepository procedimientoRepository;
    // Repositorio para crear y actualizar la entrega asociada a cada orden
    private final EntregaRepository entregaRepository;
    // Repositorio para guardar notificaciones in-app
    private final NotificacionRepository notificacionRepository;
    // Repositorio para obtener los técnicos a notificar
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Lista todas las órdenes de trabajo que tienen un estado específico.
     * Por ejemplo: listar órdenes "PENDIENTE", "EN_PROCESO" o "ENTREGADA".
     */
    public List<OrdenResponse> listarPorEstado(String estado) {
        // Busca en la BD todas las órdenes con el estado dado y convierte cada una a DTO
        return ordenRepository.findByEstado(estado).stream().map(OrdenResponse::from).toList();
    }

    /**
     * Lista todas las órdenes asociadas a un procedimiento específico.
     * Permite ver qué trabajos de laboratorio tiene un procedimiento.
     */
    public List<OrdenResponse> listarPorProcedimiento(Long procedimientoId) {
        // Busca las órdenes filtrando por el ID del procedimiento padre
        return ordenRepository.findByProcedimientoId(procedimientoId)
                .stream().map(OrdenResponse::from).toList();
    }

    /**
     * Obtiene una orden específica por su ID.
     * Lanza excepción si la orden no existe.
     */
    public OrdenResponse obtener(Long id) {
        // getOrThrow busca la orden y lanza ResourceNotFoundException si no la encuentra
        return OrdenResponse.from(getOrThrow(id));
    }

    @Transactional // Esta operación crea dos registros en la BD: la orden y su entrega
    /**
     * Crea una nueva orden de trabajo vinculada a un procedimiento.
     * Al crearse la orden, automáticamente se crea también su registro de entrega con estado PENDIENTE.
     */
    public OrdenResponse crear(OrdenRequest req) {
        // Busca el procedimiento al que se vinculará esta orden; lanza excepción si no existe
        Procedimiento procedimiento = procedimientoRepository.findById(req.idProcedimiento())
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + req.idProcedimiento() + " no encontrado"));
        // Construye la entidad Orden con el procedimiento y descripción del trabajo a realizar
        Orden orden = Orden.builder()
                .procedimiento(procedimiento)    // Asocia la orden al procedimiento odontológico
                .descripcion(req.descripcion())  // Descripción del trabajo de laboratorio a realizar
                .estado("PENDIENTE").build();    // Estado inicial: la orden está pendiente de inicio
        Orden saved = ordenRepository.save(orden); // Persiste la orden en la BD

        // Crea automáticamente el registro de entrega asociado a esta orden
        // Cada orden tiene exactamente una entrega que se actualiza cuando el trabajo esté listo
        Entrega entrega = Entrega.builder().orden(saved).estado("PENDIENTE").build();
        entregaRepository.save(entrega); // Guarda la entrega en la BD

        // Notificaciones in-app a todos los técnicos activos
        try {
            final Long ordenId = saved.getId();
            userRepository.findByRolNombreRolAndEstadoTrue("ROLE_TECNICO").forEach(tecnico -> {
                try {
                    notificacionRepository.save(Notificacion.builder()
                            .usuario(tecnico)
                            .tipo("ORDEN")
                            .mensaje("Nueva orden de laboratorio #" + ordenId + " asignada")
                            .build());
                } catch (Exception ex) {
                    log.warn("No se pudo guardar notificación de orden para técnico {}: {}", tecnico.getId(), ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("No se pudo guardar notificaciones de nueva orden: {}", e.getMessage());
        }

        // Retorna el DTO de la orden creada
        return OrdenResponse.from(saved);
    }

    @Transactional // Esta operación actualiza uno o dos registros en la BD
    /**
     * Cambia el estado de una orden (PENDIENTE → EN_PROCESO → ENTREGADA, etc.).
     * Si el nuevo estado es ENTREGADA, también actualiza la entrega asociada con la fecha actual.
     */
    public OrdenResponse cambiarEstado(Long id, String estado) {
        // Obtiene la orden existente; lanza excepción si no existe
        Orden o = getOrThrow(id);
        o.setEstado(estado); // Actualiza el estado de la orden al nuevo estado recibido
        // Si la orden se marca como ENTREGADA, también se actualiza su entrega
        if ("ENTREGADA".equals(estado)) {
            // Busca la entrega asociada a esta orden por el ID de la orden
            entregaRepository.findByOrdenId(id).ifPresent(e -> {
                e.setEstado("ENTREGADA");                    // Marca la entrega como entregada
                e.setFechaEntrega(LocalDateTime.now());      // Registra la fecha y hora exacta de la entrega
                entregaRepository.save(e);                   // Persiste los cambios en la entrega
            });
        }
        // Guarda el nuevo estado de la orden y retorna el DTO actualizado
        return OrdenResponse.from(ordenRepository.save(o));
    }

    @Transactional // Esta operación actualiza la imagen diagnóstica de la orden
    /**
     * Adjunta o actualiza la URL de la imagen diagnóstica (radiografía, foto, etc.) de una orden.
     */
    public OrdenResponse subirImagenDiagnostica(Long id, String imagen) {
        // Obtiene la orden; lanza excepción si no existe
        Orden o = getOrThrow(id);
        o.setImagenDiagnostica(imagen); // Guarda la URL de la imagen (almacenada externamente, ej. en la nube)
        // Persiste el cambio y retorna el DTO con la imagen actualizada
        return OrdenResponse.from(ordenRepository.save(o));
    }

    @Transactional
    public void agregarArchivoDiagnostico(Long id, OrdenArchivoDiagnosticoRequest request) {
        if (request.base64().length() > MAX_DIAGNOSTIC_BASE64_CHARS) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el límite de 10 MB");
        }

        String extension = request.name().substring(request.name().lastIndexOf('.') + 1);
        if (!request.mimeType().equals("model/" + extension.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo del archivo no coincide con su extensión");
        }

        byte[] content;
        try {
            content = Base64.getDecoder().decode(request.base64());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo diagnóstico no tiene un formato válido");
        }
        if (content.length == 0 || content.length > MAX_DIAGNOSTIC_FILE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el límite de 10 MB");
        }

        Orden orden = ordenRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden " + id + " no encontrada"));
        String existing = orden.getImagenDiagnostica();
        if (existing != null && existing.contains("\"id\":\"" + request.id() + "\"")) return;

        String safeName = DIAGNOSTIC_FILE_NAME_PATTERN.matcher(request.name()).replaceAll("_").trim();
        if (safeName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del archivo no es válido");
        }

        String newFile = toDiagnosticJson(Map.of(
                "id", request.id(),
                "name", safeName,
                "mimeType", request.mimeType(),
                "base64", request.base64()
        ));
        String updated;
        if (existing == null || existing.isBlank()) {
            updated = DIAGNOSTIC_MANIFEST_PREFIX + newFile + "]}";
        } else if (existing.startsWith(DIAGNOSTIC_MANIFEST_PREFIX) && existing.endsWith("]}")) {
            String files = existing.substring(DIAGNOSTIC_MANIFEST_PREFIX.length(), existing.length() - 2);
            updated = existing.substring(0, existing.length() - 2) + (files.isEmpty() ? "" : ",") + newFile + "]}";
        } else if (existing.startsWith("{")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El manifiesto actual de archivos no es válido");
        } else {
            String legacyFile = toDiagnosticJson(Map.of(
                    "name", "",
                    existing.startsWith("data:") ? "dataUrl" : "href", existing
            ));
            updated = DIAGNOSTIC_MANIFEST_PREFIX + legacyFile + "," + newFile + "]}";
        }

        orden.setImagenDiagnostica(updated);
        ordenRepository.save(orden);
    }

    private String toDiagnosticJson(Map<String, String> file) {
        try {
            return objectMapper.writeValueAsString(file);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el archivo diagnóstico", exception);
        }
    }

    /**
     * Método auxiliar privado: busca una orden por ID y lanza excepción si no existe.
     * Se reutiliza en todos los métodos que necesitan verificar que la orden exista.
     */
    private Orden getOrThrow(Long id) {
        return ordenRepository.findById(id)
                // Si el Optional está vacío, lanza excepción con mensaje descriptivo
                .orElseThrow(() -> new ResourceNotFoundException("Orden " + id + " no encontrada"));
    }
}

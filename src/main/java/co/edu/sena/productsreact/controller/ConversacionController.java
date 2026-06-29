package co.edu.sena.productsreact.controller;

// Importa los DTOs relacionados con el sistema de mensajería/chat interno
import co.edu.sena.productsreact.dto.comunicacion.MensajeRequest;  // Datos que el cliente envía para enviar un nuevo mensaje
import co.edu.sena.productsreact.dto.comunicacion.MensajeResponse; // Datos del mensaje que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.ConversacionService;      // Servicio con la lógica de negocio del sistema de conversaciones
import jakarta.validation.Valid;                                    // Activa las validaciones declaradas en los campos del DTO
import lombok.RequiredArgsConstructor;                              // Genera el constructor para inyección de dependencias
import org.springframework.http.HttpStatus;                         // Enum con códigos de estado HTTP
import org.springframework.http.ResponseEntity;                     // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize;   // Restringe acceso por rol antes de ejecutar el método
import org.springframework.web.bind.annotation.*;                   // Anotaciones REST del controlador

import java.util.List; // Permite devolver listas de mensajes
import java.util.Map;  // Permite construir y devolver respuestas JSON simples como {"id": 5}

// @RestController marca esta clase como controlador REST (serializa respuestas a JSON automáticamente)
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/conversaciones
// Este controlador gestiona el chat interno entre el personal clínico y los pacientes
@RequestMapping("/api/conversaciones")
// @RequiredArgsConstructor genera el constructor que inyecta conversacionService
@RequiredArgsConstructor
public class ConversacionController {

    // Servicio que encapsula la lógica de negocio del sistema de conversaciones y mensajería interna
    private final ConversacionService conversacionService;

    // @PostMapping("/iniciar") mapea POST /api/conversaciones/iniciar (inicia una nueva conversación)
    @PostMapping("/iniciar")
    // isAuthenticated() permite el acceso a cualquier usuario que haya iniciado sesión, sin importar su rol
    @PreAuthorize("isAuthenticated()")
    // @RequestParam Long pacienteId: extrae el ID del paciente de la URL para asociar la conversación
    // @RequestParam(required = false) Long procedimientoId: parámetro opcional para vincular la conversación a un procedimiento clínico
    public ResponseEntity<Map<String, Long>> iniciar(
            @RequestParam Long pacienteId,
            @RequestParam(required = false) Long procedimientoId) {
        // Delega al servicio la creación de una nueva conversación entre el personal y el paciente
        // Si se proporciona procedimientoId, la conversación queda vinculada a ese procedimiento específico
        Long id = conversacionService.iniciarConversacion(pacienteId, procedimientoId);
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso (conversación)
        // Map.of("id", id) construye el JSON de respuesta: {"id": 5} con el ID de la conversación recién creada
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    // @GetMapping("/{id}/mensajes") mapea GET /api/conversaciones/{id}/mensajes
    // (obtiene todos los mensajes de una conversación específica)
    @GetMapping("/{id}/mensajes")
    // Cualquier usuario autenticado puede leer los mensajes de una conversación
    @PreAuthorize("isAuthenticated()")
    // @PathVariable Long id: extrae el ID de la conversación de la URL para obtener sus mensajes
    public ResponseEntity<List<MensajeResponse>> mensajes(@PathVariable Long id) {
        // Delega al servicio la consulta de todos los mensajes de la conversación con ese ID
        // Los mensajes se devuelven ordenados cronológicamente (del más antiguo al más reciente)
        // ResponseEntity.ok() responde 200 OK con la lista de MensajeResponse
        return ResponseEntity.ok(conversacionService.mensajes(id));
    }

    // @PostMapping("/mensajes") mapea POST /api/conversaciones/mensajes (envía un nuevo mensaje en una conversación)
    @PostMapping("/mensajes")
    // Cualquier usuario autenticado puede enviar mensajes en una conversación
    @PreAuthorize("isAuthenticated()")
    // @Valid valida los campos del DTO (que el contenido no esté vacío, que exista la conversación, etc.)
    // @RequestBody MensajeRequest: deserializa el JSON con el ID de la conversación y el texto del mensaje
    public ResponseEntity<MensajeResponse> enviar(@Valid @RequestBody MensajeRequest request) {
        // Delega al servicio el envío y almacenamiento del mensaje en la conversación indicada
        // El servicio puede notificar al destinatario si hay WebSocket configurado
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso (mensaje)
        return ResponseEntity.status(HttpStatus.CREATED).body(conversacionService.enviarMensaje(request));
    }

    // @PatchMapping("/{id}/cerrar") mapea PATCH /api/conversaciones/{id}/cerrar
    // PATCH se usa para actualización parcial (solo se cambia el estado de la conversación a CERRADA)
    @PatchMapping("/{id}/cerrar")
    // Solo ADMIN u ODONTOLOGO pueden cerrar una conversación (dar por resuelta la consulta)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    // @PathVariable Long id: extrae el ID de la conversación a cerrar de la URL
    public ResponseEntity<Void> cerrar(@PathVariable Long id) {
        // Delega al servicio el cierre de la conversación: marca el estado como CERRADA
        // Una vez cerrada, no se pueden enviar más mensajes en esa conversación
        conversacionService.cerrarConversacion(id);
        // ResponseEntity.noContent().build() responde 204 NO CONTENT: operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}

package co.edu.sena.productsreact.controller;

// Importa el DTO para la respuesta de entregas
import co.edu.sena.productsreact.dto.entrega.EntregaResponse; // Datos de la entrega que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.EntregaService;      // Servicio con la lógica de negocio de entregas de trabajos de laboratorio
import lombok.RequiredArgsConstructor;                         // Genera el constructor para inyección de dependencias
import org.springframework.http.ResponseEntity;                // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize; // Restringe acceso por rol antes de ejecutar el método
import org.springframework.security.core.Authentication;       // Contiene la identidad del usuario autenticado
import org.springframework.web.bind.annotation.*;              // Anotaciones REST del controlador

import java.util.List; // Permite devolver listas de entregas

// @RestController marca esta clase como controlador REST (serializa respuestas a JSON automáticamente)
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/entregas
// Las entregas representan la entrega de trabajos de laboratorio odontológico al paciente
@RequestMapping("/api/entregas")
// @RequiredArgsConstructor genera el constructor que inyecta entregaService
@RequiredArgsConstructor
public class EntregaController {

    // Servicio que encapsula la lógica de negocio de entregas de trabajos de laboratorio
    private final EntregaService entregaService;

    // @GetMapping sin ruta adicional mapea GET /api/entregas (lista todas las entregas del sistema)
    @GetMapping
    // Solo el personal interno (ADMIN, RECEPCIONISTA, TECNICO, ODONTOLOGO) puede ver todas las entregas
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA','ROLE_TECNICO','ROLE_ODONTOLOGO')")
    public ResponseEntity<List<EntregaResponse>> listar() {
        // Delega al servicio la consulta de todas las entregas registradas en el sistema
        // ResponseEntity.ok() responde 200 OK con la lista completa de EntregaResponse
        return ResponseEntity.ok(entregaService.listar());
    }

    // @GetMapping("/orden/{id}") mapea GET /api/entregas/orden/{id} (obtiene la entrega de una orden específica)
    @GetMapping("/orden/{id}")
    // Solo personal interno puede consultar la entrega asociada a una orden de trabajo
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA','ROLE_TECNICO','ROLE_ODONTOLOGO')")
    // @PathVariable Long id: extrae el ID de la orden de la URL para buscar su entrega asociada
    public ResponseEntity<EntregaResponse> porOrden(@PathVariable Long id) {
        // Delega al servicio la búsqueda de la entrega asociada a la orden con ese ID
        // .map(ResponseEntity::ok) convierte el Optional<EntregaResponse> a ResponseEntity 200 OK si existe
        // .orElse(ResponseEntity.notFound().build()) responde 404 NOT FOUND si no hay entrega para esa orden
        return entregaService.obtenerPorOrden(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // @GetMapping("/mis-entregas") mapea GET /api/entregas/mis-entregas (el paciente ve sus propias entregas)
    @GetMapping("/mis-entregas")
    // Solo el paciente autenticado puede ver sus propias entregas
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    // Authentication auth: Spring inyecta la información del usuario autenticado para identificar al paciente
    public ResponseEntity<List<EntregaResponse>> misEntregas(Authentication auth) {
        // auth.getName() retorna el email del paciente autenticado
        // Delega al servicio la consulta de las entregas asociadas al paciente identificado por ese email
        // ResponseEntity.ok() responde 200 OK con la lista de entregas del paciente
        return ResponseEntity.ok(entregaService.listarMisEntregas(auth.getName()));
    }

    // @PatchMapping("/{id}/estado") mapea PATCH /api/entregas/{id}/estado?estado=ENTREGADO
    // PATCH se usa para actualización parcial (solo se cambia el estado de la entrega)
    @PatchMapping("/{id}/estado")
    // Solo personal operativo (ADMIN, RECEPCIONISTA, TECNICO) puede cambiar el estado de una entrega
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA','ROLE_TECNICO')")
    // @PathVariable Long id: ID de la entrega cuyo estado se quiere cambiar
    // @RequestParam String estado: nuevo estado de la entrega (ej. EN_PROCESO, LISTO, ENTREGADO)
    public ResponseEntity<EntregaResponse> cambiarEstado(@PathVariable Long id,
                                                          @RequestParam String estado) {
        // Delega al servicio el cambio de estado de la entrega; el servicio valida que el estado sea válido
        // ResponseEntity.ok() responde 200 OK con la EntregaResponse actualizada con el nuevo estado
        return ResponseEntity.ok(entregaService.cambiarEstado(id, estado));
    }
}

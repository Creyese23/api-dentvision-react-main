package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.orden.OrdenArchivoDiagnosticoRequest;
// Importa los DTOs usados para recibir y enviar datos de órdenes de trabajo de laboratorio
import co.edu.sena.productsreact.dto.orden.OrdenRequest;  // Datos que el cliente envía para crear una orden de trabajo
import co.edu.sena.productsreact.dto.orden.OrdenResponse; // Datos de la orden que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.OrdenService;    // Servicio con la lógica de negocio de órdenes de laboratorio
import jakarta.validation.Valid;                          // Activa las validaciones declaradas en los campos del DTO
import lombok.RequiredArgsConstructor;                    // Genera el constructor para inyección de dependencias
import org.springframework.http.HttpStatus;               // Enum con códigos de estado HTTP
import org.springframework.http.ResponseEntity;           // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize; // Restringe acceso por rol antes de ejecutar el método
import org.springframework.web.bind.annotation.*;         // Anotaciones REST del controlador

import java.util.List; // Permite devolver listas de órdenes

// @RestController marca esta clase como controlador REST (serializa respuestas a JSON automáticamente)
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/ordenes
// Las órdenes representan trabajos de laboratorio dental solicitados por odontólogos
@RequestMapping("/api/ordenes")
// @RequiredArgsConstructor genera el constructor que inyecta ordenService
@RequiredArgsConstructor
public class OrdenController {

    // Servicio que encapsula la lógica de negocio de las órdenes de trabajo de laboratorio
    private final OrdenService ordenService;

    // @GetMapping sin ruta adicional mapea GET /api/ordenes?estado=PENDIENTE (lista órdenes por estado)
    @GetMapping
    // Solo personal clínico y técnico puede ver las órdenes de trabajo
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    // @RequestParam con defaultValue: extrae el parámetro "estado" de la URL; si no se envía, usa "PENDIENTE" por defecto
    public ResponseEntity<List<OrdenResponse>> porEstado(@RequestParam(defaultValue = "PENDIENTE") String estado) {
        // Delega al servicio la consulta de todas las órdenes que tienen el estado dado (PENDIENTE, EN_PROCESO, COMPLETADA, etc.)
        // ResponseEntity.ok() responde 200 OK con la lista de OrdenResponse filtrada por estado
        return ResponseEntity.ok(ordenService.listarPorEstado(estado));
    }

    // @GetMapping("/procedimiento/{id}") mapea GET /api/ordenes/procedimiento/{id}
    // (órdenes asociadas a un procedimiento clínico específico)
    @GetMapping("/procedimiento/{id}")
    // Solo personal clínico y técnico puede ver las órdenes de un procedimiento
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    // @PathVariable Long id: extrae el ID del procedimiento clínico de la URL para filtrar sus órdenes
    public ResponseEntity<List<OrdenResponse>> porProcedimiento(@PathVariable Long id) {
        // Delega al servicio la consulta de órdenes asociadas al procedimiento con ese ID
        // ResponseEntity.ok() responde 200 OK con la lista de OrdenResponse del procedimiento
        return ResponseEntity.ok(ordenService.listarPorProcedimiento(id));
    }

    // @GetMapping("/{id}") mapea GET /api/ordenes/{id} (detalle de una orden específica)
    @GetMapping("/{id}")
    // Solo personal clínico y técnico puede ver el detalle de una orden
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    // @PathVariable Long id: extrae el ID de la orden de la URL
    public ResponseEntity<OrdenResponse> obtener(@PathVariable Long id) {
        // Delega al servicio la búsqueda de la orden con ese ID; lanza excepción si no existe
        // ResponseEntity.ok() responde 200 OK con los datos completos de la orden
        return ResponseEntity.ok(ordenService.obtener(id));
    }

    // @PostMapping mapea POST /api/ordenes (crea una nueva orden de trabajo de laboratorio)
    @PostMapping
    // ADMIN, ODONTOLOGO y TECNICO pueden crear órdenes de trabajo
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    // @Valid valida los campos del DTO; @RequestBody deserializa el JSON en un objeto OrdenRequest
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody OrdenRequest request) {
        // Delega al servicio la creación de la orden con los datos del request (procedimiento, tipo de trabajo, etc.)
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso (orden)
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.crear(request));
    }

    // @PatchMapping("/{id}/estado") mapea PATCH /api/ordenes/{id}/estado?estado=EN_PROCESO
    // PATCH se usa para actualización parcial (solo se modifica el estado de la orden)
    @PatchMapping("/{id}/estado")
    // Personal clínico y técnico puede actualizar el estado de una orden
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    // @PathVariable Long id: ID de la orden cuyo estado se quiere actualizar
    // @RequestParam String estado: nuevo estado de la orden (ej. EN_PROCESO, COMPLETADA, CANCELADA)
    public ResponseEntity<OrdenResponse> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        // Delega al servicio el cambio de estado de la orden; el servicio puede crear la entrega automáticamente al completarse
        // ResponseEntity.ok() responde 200 OK con la OrdenResponse actualizada con el nuevo estado
        return ResponseEntity.ok(ordenService.cambiarEstado(id, estado));
    }

    // @PatchMapping("/{id}/imagen") mapea PATCH /api/ordenes/{id}/imagen
    // PATCH se usa para actualización parcial (solo se adjunta la imagen diagnóstica a la orden)
    @PatchMapping("/{id}/imagen")
    // Solo ADMIN y ODONTOLOGO pueden adjuntar imágenes diagnósticas (radiografías, fotos, etc.) a las órdenes
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    // @PathVariable Long id: ID de la orden a la que se adjuntará la imagen
    // @RequestBody Map<String, String>: recibe el cuerpo JSON con la URL o base64 de la imagen: {"imagen": "..."}
    public ResponseEntity<OrdenResponse> subirImagen(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        // Extrae el valor de la clave "imagen" del mapa (URL o base64 de la imagen diagnóstica)
        // Delega al servicio el almacenamiento de la imagen y su asociación a la orden
        // ResponseEntity.ok() responde 200 OK con la OrdenResponse actualizada que incluye la referencia a la imagen
        return ResponseEntity.ok(ordenService.subirImagenDiagnostica(id, body.get("imagen")));
    }

    @PatchMapping("/{id}/archivos")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<Void> agregarArchivoDiagnostico(
            @PathVariable Long id,
            @Valid @RequestBody OrdenArchivoDiagnosticoRequest request
    ) {
        ordenService.agregarArchivoDiagnostico(id, request);
        return ResponseEntity.noContent().build();
    }
}

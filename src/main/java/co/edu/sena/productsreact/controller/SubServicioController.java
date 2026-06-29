package co.edu.sena.productsreact.controller;

// Importa los DTOs para recibir y enviar datos de subservicios
import co.edu.sena.productsreact.dto.servicio.SubServicioRequest;  // Datos que el cliente envía para crear o actualizar un subservicio
import co.edu.sena.productsreact.dto.servicio.SubServicioResponse; // Datos del subservicio que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.SubServicioService;       // Servicio con la lógica de negocio de subservicios
import jakarta.validation.Valid;                                    // Activa validaciones de los campos del DTO
import lombok.RequiredArgsConstructor;                              // Genera el constructor para inyección de dependencias
import org.springframework.http.HttpStatus;                         // Enum con códigos de estado HTTP
import org.springframework.http.ResponseEntity;                     // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize;   // Restringe acceso por rol antes de ejecutar el método
import org.springframework.web.bind.annotation.*;                   // Anotaciones REST del controlador

import java.util.List; // Permite devolver listas de subservicios

// @RestController marca esta clase como controlador REST (serializa respuestas a JSON automáticamente)
@RestController
// @RequestMapping con /api/servicios/{servicioId}/sub indica que los subservicios son
// un recurso anidado bajo un servicio padre: todos los endpoints incluyen el ID del servicio padre en la ruta
@RequestMapping("/api/servicios/{servicioId}/sub")
// @RequiredArgsConstructor genera el constructor que inyecta subServicioService
@RequiredArgsConstructor
public class SubServicioController {

    // Servicio que encapsula la lógica de negocio de los subservicios (variantes o especialidades de un servicio)
    private final SubServicioService subServicioService;

    // @GetMapping sin ruta adicional mapea GET /api/servicios/{servicioId}/sub (lista todos los subservicios del servicio)
    // Público: cualquier usuario puede consultar los subservicios de un servicio
    @GetMapping
    // @PathVariable Long servicioId: extrae el ID del servicio padre de la ruta URL para filtrar sus subservicios
    public ResponseEntity<List<SubServicioResponse>> listar(@PathVariable Long servicioId) {
        // Delega al servicio la consulta de todos los subservicios que pertenecen al servicio con ese ID
        // ResponseEntity.ok() responde 200 OK con la lista de SubServicioResponse
        return ResponseEntity.ok(subServicioService.listarPorServicio(servicioId));
    }

    // @PostMapping mapea POST /api/servicios/{servicioId}/sub (crea un nuevo subservicio bajo el servicio padre)
    @PostMapping
    // Solo el ADMIN puede añadir subservicios al catálogo
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long servicioId: ID del servicio padre al que se asociará el nuevo subservicio
    // @Valid + @RequestBody SubServicioRequest: valida y deserializa el JSON con nombre, precio, etc. del subservicio
    public ResponseEntity<SubServicioResponse> crear(
            @PathVariable Long servicioId,
            @Valid @RequestBody SubServicioRequest request) {
        // Delega al servicio la creación del subservicio asociándolo al servicio padre con servicioId
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso
        return ResponseEntity.status(HttpStatus.CREATED).body(subServicioService.crear(servicioId, request));
    }

    // @PutMapping("/{id}") mapea PUT /api/servicios/{servicioId}/sub/{id} (actualización completa de un subservicio)
    @PutMapping("/{id}")
    // Solo el ADMIN puede modificar subservicios existentes
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long servicioId: ID del servicio padre (necesario por la estructura de la ruta, aunque no se use en la lógica)
    // @PathVariable Long id: ID del subservicio específico a actualizar
    // @Valid + @RequestBody: valida y deserializa el JSON con los nuevos datos del subservicio
    public ResponseEntity<SubServicioResponse> actualizar(
            @PathVariable Long servicioId,
            @PathVariable Long id,
            @Valid @RequestBody SubServicioRequest request) {
        // Delega al servicio la actualización del subservicio con el ID dado; servicioId está en la ruta pero no se pasa al servicio
        // ResponseEntity.ok() responde 200 OK con el SubServicioResponse actualizado
        return ResponseEntity.ok(subServicioService.actualizar(id, request));
    }

    // @DeleteMapping("/{id}") mapea DELETE /api/servicios/{servicioId}/sub/{id} (elimina un subservicio)
    @DeleteMapping("/{id}")
    // Solo el ADMIN puede eliminar subservicios
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long servicioId: ID del servicio padre (requerido por la estructura de la ruta)
    // @PathVariable Long id: ID del subservicio específico a eliminar
    public ResponseEntity<Void> eliminar(@PathVariable Long servicioId, @PathVariable Long id) {
        // Delega al servicio la eliminación del subservicio con ese ID
        subServicioService.eliminar(id);
        // ResponseEntity.noContent().build() responde 204 NO CONTENT: operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}

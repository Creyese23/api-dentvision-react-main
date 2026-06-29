package co.edu.sena.productsreact.controller;

// Importa los DTOs usados para recibir y enviar datos de servicios odontológicos
import co.edu.sena.productsreact.dto.servicio.ServicioRequest;  // Datos que el cliente envía para crear o actualizar un servicio
import co.edu.sena.productsreact.dto.servicio.ServicioResponse; // Datos del servicio que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.ServicioService;       // Servicio con la lógica de negocio de servicios odontológicos
import jakarta.validation.Valid;                                 // Activa las validaciones declaradas en los campos del DTO
import lombok.RequiredArgsConstructor;                           // Genera el constructor que inyecta dependencias finales
import org.springframework.http.HttpStatus;                      // Enum con códigos de estado HTTP (200, 201, 204, etc.)
import org.springframework.http.ResponseEntity;                  // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize; // Permite restringir acceso por rol antes de ejecutar el método
import org.springframework.web.bind.annotation.*;                // Anotaciones REST del controlador

import java.util.List; // Permite devolver listas de servicios

// @RestController marca esta clase como controlador REST:
// serializa automáticamente los objetos de retorno a JSON sin necesitar @ResponseBody en cada método
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/servicios
@RequestMapping("/api/servicios")
// @RequiredArgsConstructor genera el constructor que inyecta servicioService
@RequiredArgsConstructor
public class ServicioController {

    // Servicio que encapsula la lógica de negocio de los servicios odontológicos (limpieza, ortodoncia, etc.)
    private final ServicioService servicioService;

    // @GetMapping sin ruta adicional mapea GET /api/servicios (lista todos los servicios activos)
    // Este endpoint es público: cualquier usuario (incluso no autenticado) puede ver el catálogo de servicios
    @GetMapping
    public ResponseEntity<List<ServicioResponse>> listar() {
        // Delega al servicio la consulta de todos los servicios con estado activo (no eliminados)
        // ResponseEntity.ok() responde 200 OK con la lista de ServicioResponse
        return ResponseEntity.ok(servicioService.listarActivos());
    }

    // @GetMapping("/{id}") mapea GET /api/servicios/{id} (obtiene un servicio específico por ID)
    // También público: cualquier usuario puede consultar el detalle de un servicio
    @GetMapping("/{id}")
    // @PathVariable Long id: extrae el valor {id} de la URL y lo convierte a Long
    public ResponseEntity<ServicioResponse> obtener(@PathVariable Long id) {
        // Delega al servicio la búsqueda del servicio con ese ID; lanza excepción si no existe
        // ResponseEntity.ok() responde 200 OK con los datos del servicio encontrado
        return ResponseEntity.ok(servicioService.obtener(id));
    }

    // @PostMapping mapea POST /api/servicios (crea un nuevo servicio odontológico)
    @PostMapping
    // Solo el ADMIN puede crear nuevos servicios en el catálogo
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @Valid valida los campos del DTO según sus anotaciones de validación
    // @RequestBody deserializa el JSON del cuerpo de la petición en un objeto ServicioRequest
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest request) {
        // Delega al servicio la creación del nuevo servicio odontológico con los datos recibidos
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(request));
    }

    // @PutMapping("/{id}") mapea PUT /api/servicios/{id} (actualización completa del servicio)
    @PutMapping("/{id}")
    // Solo el ADMIN puede modificar servicios existentes
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long id: ID del servicio a actualizar extraído de la URL
    // @Valid + @RequestBody: valida y deserializa el JSON con los nuevos datos del servicio
    public ResponseEntity<ServicioResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ServicioRequest request) {
        // Delega al servicio la actualización del servicio con el ID dado usando los nuevos datos
        // ResponseEntity.ok() responde 200 OK con el ServicioResponse actualizado
        return ResponseEntity.ok(servicioService.actualizar(id, request));
    }

    // @DeleteMapping("/{id}") mapea DELETE /api/servicios/{id} (desactiva un servicio del catálogo)
    @DeleteMapping("/{id}")
    // Solo el ADMIN puede desactivar servicios
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long id: ID del servicio a desactivar extraído de la URL
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        // Delega al servicio la desactivación (eliminación lógica) del servicio: no borra el registro, lo marca como inactivo
        servicioService.desactivar(id);
        // ResponseEntity.noContent().build() responde 204 NO CONTENT: operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}

package co.edu.sena.productsreact.controller;

// Importa los DTOs usados para recibir y enviar datos de pacientes
import co.edu.sena.productsreact.dto.paciente.PacienteRequest;   // Datos que el cliente envía para crear o actualizar un paciente
import co.edu.sena.productsreact.dto.paciente.PacienteResponse;  // Datos del paciente que el servidor devuelve al cliente
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;    // Datos de usuario usados en la lista de pendientes de activación
import co.edu.sena.productsreact.service.PacienteService;        // Servicio con la lógica de negocio de pacientes
import jakarta.validation.Valid;                                  // Activa validaciones declaradas en los DTOs (@NotBlank, @Size, etc.)
import lombok.RequiredArgsConstructor;                            // Genera automáticamente el constructor para inyectar dependencias finales
import org.springframework.data.domain.Page;                     // Wrapper de Spring Data para respuestas paginadas
import org.springframework.data.domain.Pageable;                 // Parámetros de paginación y ordenamiento extraídos automáticamente de la URL
import org.springframework.http.HttpStatus;                      // Enum con los códigos de estado HTTP estándar
import org.springframework.http.ResponseEntity;                  // Envuelve la respuesta HTTP permitiendo controlar estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize; // Permite definir reglas de autorización por rol antes de ejecutar el método
import org.springframework.security.core.Authentication;         // Contiene la información del usuario autenticado en la sesión actual
import org.springframework.web.bind.annotation.*;                // Importa todas las anotaciones REST del controlador

import java.util.List; // Permite devolver listas de objetos en las respuestas

// @RestController marca la clase como controlador REST:
// serializa automáticamente los objetos de retorno a JSON
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/pacientes
@RequestMapping("/api/pacientes")
// @RequiredArgsConstructor genera el constructor que inyecta pacienteService
@RequiredArgsConstructor
public class PacienteController {

    // Servicio que encapsula toda la lógica de negocio relacionada con pacientes
    private final PacienteService pacienteService;

    // @GetMapping("/me") mapea GET /api/pacientes/me a este método
    @GetMapping("/me")
    // @PreAuthorize restringe el acceso: solo usuarios con rol ROLE_PACIENTE pueden llamar este endpoint
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    // Authentication auth: Spring inyecta automáticamente el objeto con los datos del usuario autenticado (extraído del JWT)
    public ResponseEntity<PacienteResponse> miPaciente(Authentication auth) {
        // auth.getName() retorna el email (username) del paciente autenticado
        // Delega al servicio la búsqueda del registro de paciente asociado a ese email
        // ResponseEntity.ok() responde 200 OK con los datos del paciente
        return ResponseEntity.ok(pacienteService.obtenerMiPaciente(auth.getName()));
    }

    // @GetMapping sin ruta adicional mapea GET /api/pacientes a este método (lista todos los pacientes activos)
    @GetMapping
    // Solo ADMIN, ODONTOLOGO o RECEPCIONISTA pueden listar todos los pacientes
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA')")
    public ResponseEntity<List<PacienteResponse>> listar() {
        // Delega al servicio la consulta de todos los pacientes con estado activo
        // ResponseEntity.ok() responde 200 OK con la lista de PacienteResponse
        return ResponseEntity.ok(pacienteService.listarActivos());
    }

    // @GetMapping("/buscar") mapea GET /api/pacientes/buscar?q=texto&page=0&size=10 a este método
    @GetMapping("/buscar")
    // Solo roles administrativos y clínicos pueden buscar pacientes
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA')")
    // @RequestParam String q: extrae el parámetro de búsqueda "q" de la URL (texto a buscar en nombre, documento, etc.)
    // Pageable pageable: Spring extrae automáticamente los parámetros page, size y sort de la URL para paginar resultados
    public ResponseEntity<Page<PacienteResponse>> buscar(@RequestParam String q, Pageable pageable) {
        // Delega al servicio la búsqueda de pacientes cuyo nombre o documento contenga el texto "q"
        // Page<PacienteResponse> incluye los resultados más metadatos: total de elementos, número de página, etc.
        // ResponseEntity.ok() responde 200 OK con la página de resultados
        return ResponseEntity.ok(pacienteService.buscar(q, pageable));
    }

    // @GetMapping("/{id}") mapea GET /api/pacientes/{id} a este método (obtiene un paciente por su ID)
    @GetMapping("/{id}")
    // Todos los roles pueden ver un paciente específico (incluido el propio paciente)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    // @PathVariable Long id: extrae el valor {id} de la ruta URL y lo convierte a Long
    public ResponseEntity<PacienteResponse> obtener(@PathVariable Long id) {
        // Delega al servicio la búsqueda del paciente con ese ID; lanza excepción si no existe
        // ResponseEntity.ok() responde 200 OK con los datos del paciente encontrado
        return ResponseEntity.ok(pacienteService.obtener(id));
    }

    // @GetMapping("/pendientes-activacion") mapea GET /api/pacientes/pendientes-activacion
    @GetMapping("/pendientes-activacion")
    // ADMIN y RECEPCIONISTA pueden ver la lista de usuarios que se registraron pero aún no han sido activados como pacientes
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    public ResponseEntity<List<UsuarioResponse>> pendientesActivacion() {
        // Delega al servicio la consulta de usuarios registrados que no tienen aún un perfil de paciente activo
        // ResponseEntity.ok() responde 200 OK con la lista de UsuarioResponse pendientes
        return ResponseEntity.ok(pacienteService.listarPendientesActivacion());
    }

    // @PostMapping("/activar/{idUsuario}") mapea POST /api/pacientes/activar/{idUsuario}
    @PostMapping("/activar/{idUsuario}")
    // ADMIN y RECEPCIONISTA pueden activar un usuario como paciente
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @PathVariable Long idUsuario: extrae el ID del usuario de la URL para identificar a quién activar
    public ResponseEntity<PacienteResponse> activar(@PathVariable Long idUsuario) {
        // Delega al servicio la creación del perfil de paciente asociado al usuario con ese ID
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso (perfil de paciente)
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.activarPaciente(idUsuario));
    }

    // @PostMapping mapea POST /api/pacientes a este método (crea un nuevo paciente directamente)
    @PostMapping
    // Solo ADMIN o RECEPCIONISTA pueden crear pacientes manualmente
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @Valid valida el cuerpo según las restricciones del DTO; @RequestBody convierte el JSON a PacienteRequest
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest request) {
        // Delega al servicio la creación del paciente con los datos recibidos
        // ResponseEntity.status(CREATED) responde 201 CREATED indicando que el recurso fue creado exitosamente
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.crear(request));
    }

    // @PutMapping("/{id}") mapea PUT /api/pacientes/{id} (actualización completa del paciente)
    @PutMapping("/{id}")
    // Solo ADMIN o RECEPCIONISTA pueden modificar datos de un paciente
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @PathVariable Long id: ID del paciente a actualizar extraído de la URL
    // @Valid + @RequestBody: valida y deserializa el JSON con los nuevos datos del paciente
    public ResponseEntity<PacienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        // Delega al servicio la actualización del paciente con el ID dado usando los datos del request
        // ResponseEntity.ok() responde 200 OK con el PacienteResponse actualizado
        return ResponseEntity.ok(pacienteService.actualizar(id, request));
    }

    // @DeleteMapping("/{id}") mapea DELETE /api/pacientes/{id} (eliminación lógica del paciente)
    @DeleteMapping("/{id}")
    // Solo el ADMIN puede eliminar pacientes
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long id: extrae el ID del paciente a eliminar de la URL
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        // Delega al servicio la eliminación (lógica o física) del paciente con ese ID
        pacienteService.eliminar(id);
        // ResponseEntity.noContent().build() responde 204 NO CONTENT: operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}

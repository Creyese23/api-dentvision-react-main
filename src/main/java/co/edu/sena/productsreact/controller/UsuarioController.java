package co.edu.sena.productsreact.controller;

// Importa los DTOs relacionados con la gestión de usuarios
import co.edu.sena.productsreact.dto.usuario.CreateUsuarioRequest; // Datos que el admin envía para crear un nuevo usuario manualmente
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;      // Datos del usuario que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.UsuarioService;           // Servicio con la lógica de negocio de gestión de usuarios
import jakarta.validation.Valid;                                    // Activa las validaciones declaradas en los campos del DTO
import lombok.RequiredArgsConstructor;                              // Genera el constructor para inyección de dependencias
import org.springframework.http.HttpStatus;                         // Enum con códigos de estado HTTP
import org.springframework.http.ResponseEntity;                     // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize;   // Restringe acceso por rol antes de ejecutar el método
import org.springframework.web.bind.annotation.*;                   // Anotaciones REST del controlador

import java.util.List; // Permite devolver listas de usuarios
import java.util.Map;  // Permite recibir JSON simple como {"rol": "ROLE_ODONTOLOGO"} sin crear un DTO específico

// @RestController marca esta clase como controlador REST (serializa respuestas a JSON automáticamente)
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/usuarios
// Este controlador es exclusivamente administrativo: todos sus endpoints requieren ROLE_ADMIN
@RequestMapping("/api/usuarios")
// @RequiredArgsConstructor genera el constructor que inyecta usuarioService
@RequiredArgsConstructor
public class UsuarioController {

    // Servicio que encapsula la lógica de negocio de gestión de usuarios del sistema
    private final UsuarioService usuarioService;

    // @GetMapping sin ruta adicional mapea GET /api/usuarios (lista usuarios activos del sistema)
    @GetMapping
    // Solo el ADMIN puede listar los usuarios activos del sistema
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        // Delega al servicio la consulta de todos los usuarios con estado activo en el sistema
        // ResponseEntity.ok() responde 200 OK con la lista de UsuarioResponse (usuarios activos)
        return ResponseEntity.ok(usuarioService.listar());
    }

    // @GetMapping("/todos") mapea GET /api/usuarios/todos (lista TODOS los usuarios, incluidos los inactivos)
    @GetMapping("/todos")
    // Solo el ADMIN puede ver todos los usuarios incluyendo los desactivados
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        // Delega al servicio la consulta de todos los usuarios sin importar su estado (activos + inactivos)
        // ResponseEntity.ok() responde 200 OK con la lista completa de UsuarioResponse
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // @PatchMapping("/{id}/estado") mapea PATCH /api/usuarios/{id}/estado (activa o desactiva un usuario)
    // PATCH se usa para actualización parcial (solo se cambia el estado activo/inactivo del usuario)
    @PatchMapping("/{id}/estado")
    // Solo el ADMIN puede activar o desactivar usuarios
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long id: extrae el ID del usuario cuyo estado se quiere cambiar de la URL
    public ResponseEntity<UsuarioResponse> toggleEstado(@PathVariable Long id) {
        // Delega al servicio el cambio de estado del usuario: si estaba activo lo desactiva y viceversa (toggle)
        // ResponseEntity.ok() responde 200 OK con el UsuarioResponse actualizado con el nuevo estado
        return ResponseEntity.ok(usuarioService.toggleEstado(id));
    }

    // @PatchMapping("/{id}/rol") mapea PATCH /api/usuarios/{id}/rol (cambia el rol de un usuario)
    // PATCH se usa para actualización parcial (solo se modifica el rol asignado al usuario)
    @PatchMapping("/{id}/rol")
    // Solo el ADMIN puede cambiar el rol de un usuario
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long id: ID del usuario cuyo rol se quiere cambiar
    // @RequestBody Map<String, String> body: recibe el JSON {"rol": "ROLE_ODONTOLOGO"} sin necesitar un DTO específico
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // body.get("rol") extrae el nuevo rol del mapa JSON recibido (ej. "ROLE_ODONTOLOGO", "ROLE_RECEPCIONISTA", etc.)
        // Delega al servicio el cambio de rol del usuario con ese ID
        // ResponseEntity.ok() responde 200 OK con el UsuarioResponse actualizado con el nuevo rol
        return ResponseEntity.ok(usuarioService.cambiarRol(id, body.get("rol")));
    }

    // @PostMapping mapea POST /api/usuarios (el admin crea un nuevo usuario directamente sin proceso de registro)
    @PostMapping
    // Solo el ADMIN puede crear usuarios directamente desde el panel de administración
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @Valid valida los campos del DTO; @RequestBody deserializa el JSON en un objeto CreateUsuarioRequest
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CreateUsuarioRequest request) {
        // Delega al servicio la creación del usuario con el rol, email y contraseña especificados
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso (usuario)
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }
}

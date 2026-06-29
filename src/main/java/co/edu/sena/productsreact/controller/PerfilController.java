package co.edu.sena.productsreact.controller;

// Importa los DTOs relacionados con el perfil del usuario autenticado
import co.edu.sena.productsreact.dto.perfil.CambiarPasswordRequest; // Datos para cambiar la contraseña (contraseña actual + nueva)
import co.edu.sena.productsreact.dto.perfil.PerfilRequest;          // Datos para actualizar el perfil (nombre, teléfono, etc.)
import co.edu.sena.productsreact.dto.perfil.PerfilResponse;         // Datos del perfil que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.PerfilService;             // Servicio con la lógica de negocio del perfil de usuario
import lombok.RequiredArgsConstructor;                               // Genera el constructor para inyección de dependencias
import org.springframework.http.ResponseEntity;                      // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.core.Authentication;            // Contiene la identidad del usuario autenticado
import org.springframework.web.bind.annotation.*;                    // Anotaciones REST del controlador

// @RestController marca esta clase como controlador REST (serializa respuestas a JSON automáticamente)
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/perfil
// Este controlador gestiona el perfil del usuario que está actualmente autenticado
@RequestMapping("/api/perfil")
// @RequiredArgsConstructor genera el constructor que inyecta perfilService
@RequiredArgsConstructor
public class PerfilController {

    // Servicio que encapsula la lógica para consultar y actualizar el perfil del usuario autenticado
    private final PerfilService perfilService;

    // @GetMapping("/me") mapea GET /api/perfil/me (obtiene el perfil del usuario autenticado)
    // No tiene @PreAuthorize porque cualquier usuario autenticado puede ver su propio perfil;
    // Spring Security ya exige autenticación por defecto en rutas /api/**
    @GetMapping("/me")
    // Authentication auth: Spring inyecta automáticamente el objeto con los datos del usuario autenticado (extraído del JWT)
    public ResponseEntity<PerfilResponse> getMiPerfil(Authentication auth) {
        // auth.getName() retorna el email (username) del usuario autenticado
        // Delega al servicio la búsqueda y construcción del perfil del usuario con ese email
        // ResponseEntity.ok() responde 200 OK con los datos del perfil (nombre, email, rol, foto, etc.)
        return ResponseEntity.ok(perfilService.getMiPerfil(auth.getName()));
    }

    // @PutMapping("/me") mapea PUT /api/perfil/me (actualiza el perfil del usuario autenticado)
    @PutMapping("/me")
    // Authentication auth: identidad del usuario autenticado para saber qué perfil actualizar
    // @RequestBody PerfilRequest req: deserializa el JSON con los campos a actualizar (nombre, teléfono, foto, etc.)
    // Nota: no tiene @Valid porque la validación se hace en la capa de servicio o el DTO no tiene restricciones estrictas
    public ResponseEntity<PerfilResponse> actualizarPerfil(Authentication auth,
                                                            @RequestBody PerfilRequest req) {
        // auth.getName() retorna el email del usuario autenticado
        // Delega al servicio la actualización de los datos del perfil del usuario con ese email
        // ResponseEntity.ok() responde 200 OK con el PerfilResponse actualizado
        return ResponseEntity.ok(perfilService.actualizarPerfil(auth.getName(), req));
    }

    // @PostMapping("/me/cambiar-password") mapea POST /api/perfil/me/cambiar-password
    // Se usa POST porque implica una acción de seguridad (cambio de contraseña), no solo una actualización de datos
    @PostMapping("/me/cambiar-password")
    // Authentication auth: identidad del usuario autenticado para verificar que cambia su propia contraseña
    // @RequestBody CambiarPasswordRequest req: deserializa el JSON con la contraseña actual y la nueva contraseña
    public ResponseEntity<Void> cambiarPassword(Authentication auth,
                                                 @RequestBody CambiarPasswordRequest req) {
        // auth.getName() retorna el email del usuario autenticado
        // Delega al servicio el cambio de contraseña: verifica que la contraseña actual sea correcta antes de actualizar
        perfilService.cambiarPassword(auth.getName(), req);
        // ResponseEntity.noContent().build() responde 204 NO CONTENT: operación exitosa sin cuerpo de respuesta
        // No se devuelve ningún dato porque el cambio de contraseña no produce un recurso nuevo
        return ResponseEntity.noContent().build();
    }
}

package co.edu.sena.productsreact.controller;

// Importa los DTOs (objetos de transferencia de datos) relacionados con autenticación
import co.edu.sena.productsreact.dto.auth.AuthResponse;    // Respuesta que contiene el token JWT y datos del usuario autenticado
import co.edu.sena.productsreact.dto.auth.LoginRequest;    // Datos que el cliente envía para iniciar sesión (email + contraseña)
import co.edu.sena.productsreact.dto.auth.RegisterRequest; // Datos que el cliente envía para registrarse (nombre, email, contraseña, etc.)
import co.edu.sena.productsreact.repository.UserRepository; // Repositorio JPA para consultar usuarios directamente en base de datos
import co.edu.sena.productsreact.service.AuthService;      // Servicio que contiene la lógica de negocio de autenticación
import jakarta.validation.Valid;                            // Activa las validaciones declaradas en los DTOs (como @NotBlank, @Email)
import lombok.RequiredArgsConstructor;                      // Genera automáticamente el constructor con los campos 'final' (inyección de dependencias)
import org.springframework.http.HttpStatus;                 // Enum con los códigos de estado HTTP (200, 201, 409, etc.)
import org.springframework.http.ResponseEntity;             // Envuelve la respuesta HTTP: permite controlar el código de estado y el cuerpo
import org.springframework.web.bind.annotation.*;           // Importa todas las anotaciones REST: @RestController, @GetMapping, @PostMapping, etc.

import java.util.Map; // Permite construir respuestas JSON simples como {"message": "..."} usando Map.of(...)

// @RestController indica que esta clase es un controlador REST:
// combina @Controller (maneja peticiones HTTP) + @ResponseBody (serializa el retorno a JSON automáticamente)
@RestController
// @RequestMapping define el prefijo de ruta para todos los endpoints de esta clase:
// todas las URLs aquí comenzarán con /auth (ej. POST /auth/login, POST /auth/register)
@RequestMapping("/auth")
// @RequiredArgsConstructor genera el constructor que inyecta authService y userRepository
@RequiredArgsConstructor
public class AuthController {

    // Servicio que implementa la lógica de registro, login, verificación de correo y recuperación de contraseña
    private final AuthService authService;
    // Repositorio de usuarios: usado directamente aquí para verificar si ya existe un admin antes de crear el primero
    private final UserRepository userRepository;

    /** Crea el primer admin. Solo funciona si no existe ningún ROLE_ADMIN. */
    // @PostMapping("/setup") mapea las peticiones POST /auth/setup a este método
    @PostMapping("/setup")
    // @Valid activa las validaciones del DTO RegisterRequest antes de ejecutar el método
    // @RequestBody deserializa el JSON del cuerpo de la petición HTTP en un objeto RegisterRequest
    public ResponseEntity<?> setup(@Valid @RequestBody RegisterRequest request) {
        // Consulta la base de datos para saber si ya existe al menos un usuario con rol ROLE_ADMIN
        if (userRepository.existsByRolNombreRol("ROLE_ADMIN")) {
            // Si ya existe un admin, responde 409 CONFLICT con un mensaje descriptivo
            // Map.of("message", "...") construye un JSON: {"message": "Ya existe un administrador registrado"}
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ya existe un administrador registrado"));
        }
        // Si no existe admin, delega al servicio la creación del usuario con rol ROLE_ADMIN
        // Responde 201 CREATED con el AuthResponse que incluye el token JWT del nuevo admin
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerConRol(request, "ROLE_ADMIN"));
    }

    // @PostMapping("/register") mapea las peticiones POST /auth/register a este método
    @PostMapping("/register")
    // @Valid valida los campos del DTO; @RequestBody convierte el JSON recibido en RegisterRequest
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Delega al servicio el registro del usuario; el resultado indica si se pudo enviar correo o no
        AuthService.RegisterResult result = authService.register(request);
        // Si demoVerifyUrl no es null, significa que no hay servidor SMTP configurado
        if (result.demoVerifyUrl() != null) {
            // Responde 201 CREATED con un JSON que incluye el token, el enlace de verificación y un indicador demo
            // Map.of construye el JSON: {"auth": {...}, "verifyUrl": "...", "demo": true, "message": "..."}
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "auth", result.authResponse(),       // Objeto con el token JWT del usuario recién creado
                "verifyUrl", result.demoVerifyUrl(), // Enlace directo para verificar email sin necesitar SMTP
                "demo", true,                        // Bandera que indica al frontend que está en modo demostración
                "message", "Cuenta creada. No hay SMTP configurado — usa el enlace para verificar tu correo."
            ));
        }
        // Si hay SMTP configurado, solo se incluye el token y un mensaje informativo
        // Responde 201 CREATED: la cuenta fue creada exitosamente y se envió el correo de confirmación
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "auth", result.authResponse(),   // Objeto con el token JWT del usuario recién creado
            "message", "Cuenta creada. Revisa tu correo para confirmar tu cuenta."
        ));
    }

    // @GetMapping("/verify-email") mapea las peticiones GET /auth/verify-email a este método
    @GetMapping("/verify-email")
    // @RequestParam("token") extrae el parámetro "token" de la URL: GET /auth/verify-email?token=abc123
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        // Delega al servicio la validación del token de verificación y la activación del correo del usuario
        authService.verifyEmail(token);
        // Responde 200 OK con un mensaje de éxito; Map.of genera {"message": "Correo verificado exitosamente..."}
        return ResponseEntity.ok(Map.of("message", "Correo verificado exitosamente. Ya puedes iniciar sesión."));
    }

    // @PostMapping("/login") mapea las peticiones POST /auth/login a este método
    @PostMapping("/login")
    // @Valid valida email y contraseña antes de procesar; @RequestBody deserializa el JSON en LoginRequest
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Delega al servicio la autenticación; si las credenciales son correctas retorna un AuthResponse con JWT
        // ResponseEntity.ok() responde 200 OK con el AuthResponse (token JWT + datos del usuario)
        return ResponseEntity.ok(authService.login(request));
    }

    // @PostMapping("/refresh") mapea las peticiones POST /auth/refresh a este método
    @PostMapping("/refresh")
    // @RequestBody recibe un JSON simple: {"refreshToken": "eyJ..."}, lo convierte en un Map<String, String>
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        // Extrae el valor de la clave "refreshToken" del mapa y lo pasa al servicio para generar un nuevo JWT
        // ResponseEntity.ok() responde 200 OK con el nuevo AuthResponse que contiene el token renovado
        return ResponseEntity.ok(authService.refreshToken(body.get("refreshToken")));
    }

    // @PostMapping("/forgot-password") mapea las peticiones POST /auth/forgot-password a este método
    @PostMapping("/forgot-password")
    // @RequestBody recibe un JSON simple: {"email": "usuario@correo.com"}, convertido a Map<String, String>
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        // Extrae el email del mapa y lo envía al servicio para generar un token de restablecimiento de contraseña
        // Si no hay SMTP, el servicio retorna la URL del enlace directo; si hay SMTP, retorna null
        String resetUrl = authService.forgotPassword(body.get("email"));
        if (resetUrl != null) {
            // Sin SMTP configurado — devolver enlace directo (modo demo)
            // Responde 200 OK con un JSON que contiene el enlace para restablecer contraseña directamente
            // Map.of construye: {"message": "...", "resetUrl": "...", "demo": true}
            return ResponseEntity.ok(Map.of(
                "message", "No hay servidor de correo configurado. Usa este enlace para restablecer tu contraseña:",
                "resetUrl", resetUrl, // URL directa con el token incrustado para redirigir al formulario de nueva contraseña
                "demo", true          // Indica al frontend que no hubo envío de correo real
            ));
        }
        // Si hay SMTP, el correo ya fue enviado; responde 200 OK con un mensaje informativo
        // Map.of genera: {"message": "Te hemos enviado un enlace..."}
        return ResponseEntity.ok(Map.of("message", "Te hemos enviado un enlace de recuperación a tu correo."));
    }

    // @PostMapping("/reset-password") mapea las peticiones POST /auth/reset-password a este método
    @PostMapping("/reset-password")
    // @RequestBody recibe {"token": "abc123", "newPassword": "nuevaClave"}, convertido a Map<String, String>
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        // Extrae el token de restablecimiento y la nueva contraseña del mapa y los pasa al servicio
        // El servicio valida el token, lo marca como usado y actualiza la contraseña del usuario
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        // Responde 200 OK con un mensaje confirmando que la contraseña fue actualizada
        // Map.of genera: {"message": "Contraseña actualizada correctamente."}
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
    }
}

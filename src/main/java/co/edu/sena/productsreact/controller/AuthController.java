package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.auth.AuthResponse;
import co.edu.sena.productsreact.dto.auth.LoginRequest;
import co.edu.sena.productsreact.dto.auth.RegisterRequest;
import co.edu.sena.productsreact.repository.UserRepository;
import co.edu.sena.productsreact.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /** Crea el primer admin. Solo funciona si no existe ningún ROLE_ADMIN. */
    @PostMapping("/setup")
    public ResponseEntity<?> setup(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByRolNombreRol("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ya existe un administrador registrado"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerConRol(request, "ROLE_ADMIN"));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refreshToken(body.get("refreshToken")));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String resetUrl = authService.forgotPassword(body.get("email"));
        if (resetUrl != null) {
            // Sin SMTP configurado — devolver enlace directo (modo demo)
            return ResponseEntity.ok(Map.of(
                "message", "No hay servidor de correo configurado. Usa este enlace para restablecer tu contraseña:",
                "resetUrl", resetUrl,
                "demo", true
            ));
        }
        return ResponseEntity.ok(Map.of("message", "Te hemos enviado un enlace de recuperación a tu correo."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
    }
}

package co.edu.sena.productsreact.security;

// DTO con los tokens (access y refresh) y datos del usuario autenticado
import co.edu.sena.productsreact.dto.auth.AuthResponse;
// Servicio de autenticación que gestiona login y registro de usuarios OAuth2
import co.edu.sena.productsreact.service.AuthService;
// Excepción de Servlet que puede lanzar el manejador
import jakarta.servlet.ServletException;
// Petición HTTP entrante
import jakarta.servlet.http.HttpServletRequest;
// Respuesta HTTP que se enviará al cliente
import jakarta.servlet.http.HttpServletResponse;
// Lombok: genera el constructor con los campos final (inyección por constructor)
import lombok.RequiredArgsConstructor;
// @Value: inyecta valores desde application.properties
import org.springframework.beans.factory.annotation.Value;
// Authentication: objeto de Spring Security con los datos del usuario autenticado via OAuth2
import org.springframework.security.core.Authentication;
// Token de autenticación OAuth2 que contiene el provider y los atributos del usuario
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
// Clase base de Spring Security para redirigir al usuario tras una autenticación exitosa
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
// @Component: declara este bean para que Spring lo inyecte en SecurityConfig
import org.springframework.stereotype.Component;
// Utilidad para construir URLs con query params de forma segura (escapa caracteres especiales)
import org.springframework.web.util.UriComponentsBuilder;

// IOException: excepción de entrada/salida que puede lanzar el manejador
import java.io.IOException;
// Map: mapa de atributos del usuario OAuth2 (email, name, login, etc.)
import java.util.Map;

// @Component: Spring detecta y gestiona este bean automáticamente
@Component
// @RequiredArgsConstructor: genera constructor con authService (inyección por constructor)
@RequiredArgsConstructor
// Extiende SimpleUrlAuthenticationSuccessHandler: maneja la redirección tras autenticación OAuth2 exitosa
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // Servicio que busca o crea el usuario en la BD y genera los tokens JWT
    private final AuthService authService;

    // URL del frontend donde se redirigirá al usuario tras el login OAuth2 exitoso
    // Se inyecta desde la propiedad app.frontend-url del application.properties
    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Método llamado por Spring Security cuando el login OAuth2 fue exitoso
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Convierte el Authentication genérico al tipo específico de OAuth2 para acceder a sus datos
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        // Obtiene el mapa de atributos del usuario retornados por el proveedor (Google, GitHub)
        Map<String, Object> attrs = token.getPrincipal().getAttributes();
        // Obtiene el nombre del proveedor en mayúsculas: "GOOGLE" o "GITHUB"
        String provider = token.getAuthorizedClientRegistrationId().toUpperCase();

        // Extrae el email del usuario desde los atributos del proveedor OAuth2
        String email = extractEmail(attrs, provider);
        // Extrae el nombre completo del usuario desde los atributos del proveedor
        String nombre = extractName(attrs);

        // Busca o registra el usuario en la BD y genera los tokens JWT (access + refresh)
        AuthResponse auth = authService.loginOrRegisterOAuth(email, provider, nombre);

        // Construye la URL de redirección hacia el frontend con los tokens y datos del usuario
        // como query parameters para que el frontend los lea y guarde en localStorage/cookies
        String redirect = UriComponentsBuilder
                // URL base: frontendUrl + "/oauth2/callback" (ej: http://localhost:5173/oauth2/callback)
                .fromUriString(frontendUrl + "/oauth2/callback")
                // Agrega el access token JWT como query param (el frontend lo usará en el header Authorization)
                .queryParam("accessToken",  auth.accessToken())
                // Agrega el refresh token para que el frontend pueda renovar el access token
                .queryParam("refreshToken", auth.refreshToken())
                // ID del usuario en la BD
                .queryParam("userId",       auth.user().id())
                // Email del usuario para mostrarlo en la UI
                .queryParam("email",        auth.user().email())
                // Rol del usuario (ej: "ROLE_PACIENTE") para controlar la navegación en el frontend
                .queryParam("rol",          auth.user().rol())
                // Nombres del usuario; si es null se envía cadena vacía para evitar el texto "null" en la URL
                .queryParam("nombres",      auth.user().nombres() != null ? auth.user().nombres() : "")
                // Apellidos del usuario; si es null se envía cadena vacía
                .queryParam("apellidos",    auth.user().apellidos() != null ? auth.user().apellidos() : "")
                // Construye y convierte a String la URL con todos los query params codificados
                .build().toUriString();

        // Redirige al navegador del usuario hacia la URL del frontend construida arriba
        getRedirectStrategy().sendRedirect(request, response, redirect);
    }

    // Extrae el email del usuario desde los atributos del proveedor OAuth2
    // Cada proveedor puede retornar el email con diferentes claves en el mapa de atributos
    private String extractEmail(Map<String, Object> attrs, String provider) {
        // Intenta obtener el email desde el atributo estándar "email" (Google y GitHub con email público)
        Object email = attrs.get("email");
        // Si el email existe y no está vacío, lo retorna directamente
        if (email != null && !email.toString().isBlank()) return email.toString();
        // GitHub puede no exponer email público — usamos login como fallback
        // El "login" es el nombre de usuario de GitHub (ej: "johndoe")
        Object login = attrs.get("login");
        // Construye un email falso con el dominio noreply de GitHub para evitar conflictos
        if (login != null) return login + "@github.noreply.com";
        // Si ningún proveedor entregó email ni login, lanza excepción para evitar crear usuario sin email
        throw new IllegalStateException("No se pudo obtener el email del proveedor " + provider);
    }

    // Extrae el nombre completo del usuario desde los atributos del proveedor
    // El atributo "name" está disponible tanto en Google como en GitHub
    private String extractName(Map<String, Object> attrs) {
        Object name = attrs.get("name");
        // Si el proveedor no retornó nombre (puede ocurrir en GitHub sin nombre público), retorna cadena vacía
        return name != null ? name.toString() : "";
    }
}

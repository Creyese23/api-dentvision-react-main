package co.edu.sena.productsreact.security;

import co.edu.sena.productsreact.dto.auth.AuthResponse;
import co.edu.sena.productsreact.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attrs = token.getPrincipal().getAttributes();
        String provider = token.getAuthorizedClientRegistrationId().toUpperCase();

        String email = extractEmail(attrs, provider);
        String nombre = extractName(attrs);

        AuthResponse auth = authService.loginOrRegisterOAuth(email, provider, nombre);

        String redirect = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken",  auth.accessToken())
                .queryParam("refreshToken", auth.refreshToken())
                .queryParam("userId",       auth.user().id())
                .queryParam("email",        auth.user().email())
                .queryParam("rol",          auth.user().rol())
                .queryParam("nombres",      auth.user().nombres() != null ? auth.user().nombres() : "")
                .queryParam("apellidos",    auth.user().apellidos() != null ? auth.user().apellidos() : "")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirect);
    }

    private String extractEmail(Map<String, Object> attrs, String provider) {
        Object email = attrs.get("email");
        if (email != null && !email.toString().isBlank()) return email.toString();
        // GitHub puede no exponer email público — usamos login como fallback
        Object login = attrs.get("login");
        if (login != null) return login + "@github.noreply.com";
        throw new IllegalStateException("No se pudo obtener el email del proveedor " + provider);
    }

    private String extractName(Map<String, Object> attrs) {
        Object name = attrs.get("name");
        return name != null ? name.toString() : "";
    }
}

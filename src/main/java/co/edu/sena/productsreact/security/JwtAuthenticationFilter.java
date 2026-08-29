package co.edu.sena.productsreact.security;

// FilterChain: cadena de filtros de Servlet; permite pasar la petición al siguiente filtro
import jakarta.servlet.FilterChain;
// ServletException: excepción de Servlet que puede lanzar el filtro
import jakarta.servlet.ServletException;
// Representaciones de la petición HTTP entrante y la respuesta HTTP saliente
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// Lombok: genera automáticamente el constructor con los campos final (inyección por constructor)
import lombok.RequiredArgsConstructor;
// @NonNull: anotación de Spring que indica que el parámetro no puede ser null
import org.springframework.lang.NonNull;
// Token de autenticación de Spring Security basado en usuario y contraseña (o claims en este caso)
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// SecurityContextHolder: contenedor estático que guarda la autenticación del usuario actual (por hilo)
import org.springframework.security.core.context.SecurityContextHolder;
// UserDetails: contrato de Spring Security que representa al usuario autenticado (email, password, roles)
import org.springframework.security.core.userdetails.UserDetails;
// Construye los detalles de la petición web (IP, sessionId) para enriquecer el token de autenticación
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// @Component: declara esta clase como componente gestionado por Spring (bean detectado automáticamente)
import org.springframework.stereotype.Component;
// OncePerRequestFilter: garantiza que este filtro se ejecuta exactamente una vez por petición HTTP
import org.springframework.web.filter.OncePerRequestFilter;

// IOException: excepción de entrada/salida que puede lanzar el filtro al escribir la respuesta
import java.io.IOException;

// @Component: Spring detecta y registra este filtro automáticamente en el contexto
@Component
// @RequiredArgsConstructor: genera un constructor con jwtService y userDetailsService (inyección por constructor)
@RequiredArgsConstructor
// Extiende OncePerRequestFilter: asegura que el filtro corre exactamente una vez por request
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Servicio que genera y valida tokens JWT
    private final JwtService jwtService;
    // Servicio que carga los datos del usuario desde la base de datos dado su email
    private final CustomUserDetailsService userDetailsService;

    // Método principal del filtro: se ejecuta en cada petición HTTP antes de llegar al controlador
    @Override
    protected void doFilterInternal(
            // Petición HTTP entrante (contiene headers, body, parámetros, etc.)
            @NonNull HttpServletRequest request,
            // Respuesta HTTP que se enviará de vuelta al cliente
            @NonNull HttpServletResponse response,
            // Cadena de filtros: permite pasar la petición al siguiente filtro o al controlador
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Lee el header "Authorization" de la petición (ej: "Bearer eyJhbGciOiJIUzI1NiJ9...")
        final String authHeader = request.getHeader("Authorization");

        // Si el header no existe o no comienza con "Bearer ", no es una petición con JWT
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pasa la petición al siguiente filtro sin autenticar (puede ser una ruta pública)
            filterChain.doFilter(request, response);
            return; // Termina la ejecución de este filtro
        }

        // Extrae el token JWT eliminando el prefijo "Bearer " (los primeros 7 caracteres)
        final String jwt = authHeader.substring(7);

        try {
            // Extrae el email (subject) del payload del token JWT
            final String username = jwtService.extractUsername(jwt);

            // Siempre que llegue un Bearer token válido, se re-autentica con él, sin importar si
            // el SecurityContext ya tenía una Authentication cargada. La política de sesión
            // IF_REQUIRED (necesaria para el flujo OAuth2) hace que Spring restaure la
            // Authentication guardada en la sesión (JSESSIONID) ANTES de que corra este filtro;
            // si se omitía re-autenticar cuando ya había algo en el contexto, un usuario que
            // inicia sesión con otra cuenta en el mismo navegador (misma cookie de sesión) seguía
            // siendo tratado como el usuario de la sesión anterior pese a enviar un JWT distinto.
            if (username != null) {

                // Carga los datos completos del usuario desde la BD usando su email
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Valida que el JWT sea auténtico (firma correcta) y no haya vencido
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Crea el objeto de autenticación con el usuario, sin contraseña (null) y con sus roles
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,          // El principal (usuario autenticado)
                                    null,                 // Credenciales: null porque ya validamos el JWT
                                    userDetails.getAuthorities()); // Los roles/permisos del usuario
                    // Enriquece el token con detalles de la petición web (IP, sessionId)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    // Registra la autenticación en el SecurityContext del hilo actual
                    // A partir de aquí, Spring Security sabe que este usuario está autenticado
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Si hay cualquier error al validar el JWT (token corrupto, expirado, etc.)
            // Solo registra el mensaje en el log de depuración sin interrumpir la cadena de filtros
            logger.debug("No se pudo validar el JWT: " + ex.getMessage());
        }

        // Pasa la petición al siguiente filtro en la cadena (o al controlador si no hay más filtros)
        filterChain.doFilter(request, response);
    }
}

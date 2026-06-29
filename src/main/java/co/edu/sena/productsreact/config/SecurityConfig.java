package co.edu.sena.productsreact.config;

// Filtro personalizado que valida el JWT en cada petición HTTP
import co.edu.sena.productsreact.security.JwtAuthenticationFilter;
// Manejador que redirige al frontend con los tokens tras un login OAuth2 exitoso
import co.edu.sena.productsreact.security.OAuth2SuccessHandler;
// Lombok: genera el constructor con los campos final (inyección por constructor)
import lombok.RequiredArgsConstructor;
// @Value: inyecta valores desde application.properties en los campos anotados
import org.springframework.beans.factory.annotation.Value;
// @Bean: indica que el método produce un bean gestionado por el contenedor de Spring
import org.springframework.context.annotation.Bean;
// @Configuration: marca la clase como fuente de definiciones de beans de configuración
import org.springframework.context.annotation.Configuration;
// @EnableMethodSecurity: habilita la seguridad a nivel de método con @PreAuthorize, @Secured, etc.
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// HttpSecurity: objeto principal para configurar la seguridad HTTP de Spring Security
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// AbstractHttpConfigurer::disable: atajo para desactivar configuraciones como CSRF
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// SessionCreationPolicy: controla cuándo y cómo se crean las sesiones HTTP
import org.springframework.security.config.http.SessionCreationPolicy;
// Repositorio de registros de clientes OAuth2 (Google, GitHub) configurados en application.properties
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
// SecurityFilterChain: cadena de filtros de seguridad que procesa cada petición HTTP
import org.springframework.security.web.SecurityFilterChain;
// Filtro estándar de Spring Security que procesa autenticación por usuario/contraseña en formularios
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// Clases para configurar CORS (Cross-Origin Resource Sharing) — permite peticiones desde otros dominios
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// List: para definir listas de métodos y orígenes permitidos en CORS
import java.util.List;

// @Configuration: Spring procesa esta clase al arrancar para crear los beans de configuración
@Configuration
// @EnableMethodSecurity: activa anotaciones de seguridad a nivel de método como @PreAuthorize("hasRole('ADMIN')")
@EnableMethodSecurity
// @RequiredArgsConstructor: genera el constructor con los tres campos final como parámetros
@RequiredArgsConstructor
public class SecurityConfig {

    // Filtro que intercepta cada petición y valida el token JWT en el header Authorization
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    // Manejador que se ejecuta cuando el login OAuth2 (Google/GitHub) fue exitoso
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    // Repositorio con la configuración de clientes OAuth2 (client-id, client-secret, etc.)
    private final ClientRegistrationRepository clientRegistrationRepository;

    // URL del frontend: usada para redirigir en OAuth2 y para la configuración de CORS
    // Default: http://localhost:5173 si la propiedad no está definida
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // Client ID de Google OAuth2 inyectado desde application.properties. Vacío si no está configurado
    @Value("${app.oauth2.google.client-id:}")
    private String googleClientId;

    // Client ID de GitHub OAuth2 inyectado desde application.properties. Vacío si no está configurado
    @Value("${app.oauth2.github.client-id:}")
    private String githubClientId;

    // @Bean: Spring crea y registra esta cadena de seguridad como bean principal de seguridad HTTP
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configura CORS usando la fuente de configuración definida en corsConfigurationSource()
                // Esto permite que el frontend en otro origen (ej: localhost:5173) haga peticiones
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Desactiva CSRF porque la API usa JWT (tokens stateless), no cookies de sesión
                // CSRF solo es necesario con autenticación basada en cookies de sesión
                .csrf(AbstractHttpConfigurer::disable)
                // Política de sesión: IF_REQUIRED permite sesiones solo cuando OAuth2 las necesita
                // Para peticiones con JWT no se crea sesión (el JWT es el estado de autenticación)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // Define qué rutas son públicas y cuáles requieren autenticación
                .authorizeHttpRequests(auth -> auth
                        // Rutas de autenticación (login, registro, verificar email, etc.) son públicas
                        .requestMatchers("/auth/**").permitAll()
                        // Rutas del flujo OAuth2 (inicio y callback) son públicas para permitir el redirect
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Listado de servicios y subservicios es público (para mostrar en la landing page)
                        .requestMatchers("/api/servicios", "/api/servicios/*/sub").permitAll()
                        // Ruta raíz y página de error son accesibles sin autenticación
                        .requestMatchers("/", "/error").permitAll()
                        // Cualquier otra ruta requiere que el usuario esté autenticado (con JWT válido)
                        .anyRequest().authenticated()
                )
                // Inserta el filtro JWT ANTES del filtro estándar de usuario/contraseña
                // Así, si la petición trae un JWT válido, ya queda autenticada antes de llegar al filtro de formulario
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Solo habilitar oauth2Login si hay al menos un proveedor real configurado
        // Evita errores de configuración cuando los client-id están vacíos (ambiente de desarrollo sin OAuth2)
        boolean hasOAuth2 = isConfigured(googleClientId) || isConfigured(githubClientId);
        if (hasOAuth2) {
            // Configura el flujo de login OAuth2 (Google/GitHub)
            http.oauth2Login(oauth -> oauth
                    // Usa el repositorio con la configuración de los proveedores OAuth2
                    .clientRegistrationRepository(clientRegistrationRepository)
                    // Tras login exitoso, llama a OAuth2SuccessHandler para redirigir al frontend con tokens
                    .successHandler(oAuth2SuccessHandler)
                    // Si el login OAuth2 falla, redirige al frontend con un mensaje de error
                    .failureUrl(frontendUrl + "/login?error=oauth2")
            );
        }

        // Construye y retorna la cadena de filtros de seguridad configurada
        return http.build();
    }

    // Verifica si un valor de configuración está presente y no es una cadena vacía
    // Se usa para saber si Google o GitHub están configurados como proveedores OAuth2
    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    // @Bean: crea y registra la configuración de CORS como bean de Spring
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Crea el objeto de configuración CORS
        CorsConfiguration config = new CorsConfiguration();
        // Permite peticiones desde cualquier origen (útil para desarrollo; en producción especificar el dominio)
        config.setAllowedOriginPatterns(List.of("*"));
        // Métodos HTTP permitidos para peticiones cross-origin
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Permite cualquier header en las peticiones (incluyendo Authorization con el JWT)
        config.setAllowedHeaders(List.of("*"));
        // Expone el header Authorization en la respuesta para que el frontend pueda leerlo
        config.setExposedHeaders(List.of("Authorization"));
        // Permite el envío de credenciales (cookies, headers de autorización) en peticiones cross-origin
        config.setAllowCredentials(true);

        // Aplica la configuración de CORS a todas las rutas de la API ("/**")
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}

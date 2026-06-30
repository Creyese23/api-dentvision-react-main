package co.edu.sena.productsreact.config;

// @Bean: marca métodos cuyo valor de retorno será registrado como bean de Spring
import org.springframework.context.annotation.Bean;
// @Configuration: marca la clase como fuente de beans de configuración de Spring
import org.springframework.context.annotation.Configuration;
// AuthenticationManager: orquesta la autenticación (delega en el AuthenticationProvider correcto)
import org.springframework.security.authentication.AuthenticationManager;
// AuthenticationConfiguration: clase de Spring que expone el AuthenticationManager configurado
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// PasswordEncoder: interfaz para codificar y verificar contraseñas de forma segura
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// PasswordEncoder: interfaz base que Spring Security usa para no depender de un algoritmo específico
import org.springframework.security.crypto.password.PasswordEncoder;

// @Configuration: Spring procesa esta clase al arrancar y registra los beans definidos en ella
@Configuration
public class AppConfig {

    // @Bean: crea y registra un PasswordEncoder basado en BCrypt como bean de Spring
    // BCrypt es un algoritmo de hashing adaptativo con sal aleatoria incluida en el hash
    // Se inyecta en AuthService para hashear contraseñas al registrar usuarios
    // También lo usa Spring Security internamente para comparar contraseñas al autenticar
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder usa BCrypt con factor de trabajo por defecto de 10 iteraciones
        // A mayor factor de trabajo, más seguro pero más lento (protege contra ataques de fuerza bruta)
        return new BCryptPasswordEncoder();
    }

    // @Bean: crea y registra el AuthenticationManager como bean de Spring
    // AuthenticationManager es el punto de entrada principal para autenticar usuarios en Spring Security
    // Se inyecta en AuthService para autenticar credenciales (email/contraseña) en el login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Obtiene el AuthenticationManager ya configurado por Spring Security
        // Spring lo configura automáticamente usando el CustomUserDetailsService y el PasswordEncoder.
        return config.getAuthenticationManager();
    }
}

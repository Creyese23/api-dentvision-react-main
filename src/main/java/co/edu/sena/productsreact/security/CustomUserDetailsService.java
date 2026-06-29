package co.edu.sena.productsreact.security;

// Importa la entidad User para acceder a sus datos desde la BD
import co.edu.sena.productsreact.entity.User;
// Repositorio JPA para buscar usuarios en la base de datos
import co.edu.sena.productsreact.repository.UserRepository;
// Lombok: genera automáticamente el constructor con los campos final (inyección por constructor)
import lombok.RequiredArgsConstructor;
// SimpleGrantedAuthority: implementación simple de GrantedAuthority que representa un rol como String
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// UserDetails: contrato de Spring Security que agrupa credenciales y roles del usuario autenticado
import org.springframework.security.core.userdetails.UserDetails;
// UserDetailsService: interfaz que Spring Security llama para cargar datos del usuario por su username
import org.springframework.security.core.userdetails.UserDetailsService;
// UsernameNotFoundException: excepción que se lanza cuando no se encuentra el usuario en la BD
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// @Service: declara esta clase como un servicio de Spring (bean gestionado por el contenedor)
import org.springframework.stereotype.Service;

// List.of: crea una lista inmutable con un solo elemento (el rol del usuario)
import java.util.List;

// @Service: Spring crea y gestiona una única instancia de esta clase (singleton)
@Service
// @RequiredArgsConstructor: genera el constructor con userRepository como argumento (inyección)
@RequiredArgsConstructor
// Implementa UserDetailsService: contrato que Spring Security usa para autenticar usuarios
public class CustomUserDetailsService implements UserDetailsService {

    // Repositorio para buscar el usuario en la base de datos por su email
    private final UserRepository userRepository;

    // Método requerido por UserDetailsService: Spring Security lo llama con el "username"
    // En este proyecto el "username" es el email del usuario (no un nombre de usuario)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Busca el usuario en la BD por su email
        // orElseThrow: si no existe, lanza UsernameNotFoundException y Spring Security retorna 401
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // Construye y retorna un objeto UserDetails estándar de Spring Security con:
        // - email como username (identificador principal)
        // - password hasheado almacenado en BD (Spring Security lo comparará con BCrypt)
        // - lista de authorities (roles) del usuario: solo uno por usuario en este proyecto
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),   // username: email del usuario
                user.getPassword(), // password hasheado BCrypt
                // Convierte el nombre del rol (ej: "ROLE_ADMIN") en un GrantedAuthority de Spring Security
                List.of(new SimpleGrantedAuthority(user.getRol().getNombreRol()))
        );
    }
}

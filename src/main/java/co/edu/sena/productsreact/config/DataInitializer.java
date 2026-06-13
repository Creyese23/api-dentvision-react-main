package co.edu.sena.productsreact.config;

import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
import co.edu.sena.productsreact.repository.RolRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final RolRepository rolRepository;
    private final UserRepository userRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> ROLES = List.of(
            "ROLE_ADMIN",
            "ROLE_ODONTOLOGO",
            "ROLE_RECEPCIONISTA",
            "ROLE_TECNICO",
            "ROLE_PACIENTE"
    );

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedRoles();
            seedAdminUser();
        } catch (Exception ex) {
            // El seed no debe impedir que la aplicación arranque
            log.error("Error durante el seed inicial de datos: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void seedRoles() {
        for (String nombreRol : ROLES) {
            if (rolRepository.findByNombreRol(nombreRol).isEmpty()) {
                rolRepository.save(Rol.builder().nombreRol(nombreRol).build());
                log.info("Rol creado: {}", nombreRol);
            }
        }
    }

    @Transactional
    public void seedAdminUser() {
        String adminEmail = "admin@dentvision.co";
        if (userRepository.existsByEmail(adminEmail)) {
            log.debug("Usuario admin ya existe, omitiendo seed.");
            return;
        }

        Rol rolAdmin = rolRepository.findByNombreRol("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN no encontrado después del seed de roles"));

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin1234!"))
                .rol(rolAdmin)
                .estado(true)
                .build();
        admin = userRepository.save(admin);

        // Se proveen cadenas vacías en todos los campos para evitar
        // errores de NOT NULL en columnas de perfiles_usuario
        PerfilUsuario perfil = PerfilUsuario.builder()
                .usuario(admin)
                .nombres("Administrador")
                .apellidos("DentVision")
                .tipoIdentificacion("NIT")
                .identificacion("000000000")
                .telefono("3001234567")
                .direccion("")
                .build();
        perfilRepository.save(perfil);

        log.info("Usuario admin creado: {}", adminEmail);
    }
}

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
            "ROLE_ADMIN", "ROLE_ODONTOLOGO", "ROLE_RECEPCIONISTA",
            "ROLE_TECNICO", "ROLE_PACIENTE"
    );

    @Override
    public void run(ApplicationArguments args) {
        // 1. Roles — data.sql los inserta si spring.sql.init.mode=always;
        //    aquí los creamos como respaldo por si data.sql está desactivado.
        try {
            for (String nombreRol : ROLES) {
                if (rolRepository.findByNombreRol(nombreRol).isEmpty()) {
                    rolRepository.save(Rol.builder().nombreRol(nombreRol).build());
                    log.info("Rol creado: {}", nombreRol);
                }
            }
        } catch (Exception ex) {
            log.error("Error al sembrar roles: {}", ex.getMessage());
        }

        // 2. Usuario admin — creado via JPA para que maneje correctamente
        //    la relación con perfiles_usuario.
        try {
            String adminEmail = "admin@dentvision.co";
            if (!userRepository.existsByEmail(adminEmail)) {
                Rol rolAdmin = rolRepository.findByNombreRol("ROLE_ADMIN")
                        .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN no encontrado"));

                User admin = userRepository.save(User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode("Admin1234!"))
                        .rol(rolAdmin)
                        .estado(true)
                        .build());

                perfilRepository.save(PerfilUsuario.builder()
                        .usuario(admin)
                        .nombres("Administrador")
                        .apellidos("DentVision")
                        .tipoIdentificacion("NIT")
                        .identificacion("000000000")
                        .telefono("3001234567")
                        .direccion("")
                        .build());

                log.info("Usuario admin creado: {}", adminEmail);
            }
        } catch (Exception ex) {
            log.error("Error al crear usuario admin: {}", ex.getMessage());
        }
    }
}

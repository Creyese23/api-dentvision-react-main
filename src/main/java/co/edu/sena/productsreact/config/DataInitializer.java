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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

    private static final List<String> ROLES = List.of(
            "ROLE_ADMIN", "ROLE_ODONTOLOGO", "ROLE_RECEPCIONISTA",
            "ROLE_TECNICO", "ROLE_PACIENTE"
    );

    @Override
    public void run(ApplicationArguments args) {
        fixPerfilUsuarioColumns();
        dropTriggerIfExists();
        seedRoles();
        seedAdminUser();
    }

    /** Hace nullable con DEFAULT vacío todas las columnas opcionales de perfiles_usuario.
     *  Esto también deshabilita cualquier trigger que fallara por NOT NULL. */
    private void fixPerfilUsuarioColumns() {
        List<String> alters = List.of(
            "ALTER TABLE perfiles_usuario MODIFY COLUMN nombres VARCHAR(100) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN apellidos VARCHAR(100) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN tipo_identificacion VARCHAR(30) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN identificacion VARCHAR(20) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN telefono VARCHAR(20) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN direccion VARCHAR(200) NULL DEFAULT ''"
        );
        for (String sql : alters) {
            try {
                jdbcTemplate.execute(sql);
                log.debug("Ejecutado: {}", sql);
            } catch (Exception ex) {
                log.warn("No se pudo ejecutar '{}': {}", sql, ex.getMessage());
            }
        }
    }

    /** Elimina triggers conocidos que puedan interferir con el INSERT de usuarios. */
    private void dropTriggerIfExists() {
        List<String> triggers = List.of(
            "after_insert_usuario",
            "trg_crear_perfil",
            "crear_perfil_usuario",
            "trg_perfil_usuario",
            "after_usuario_insert",
            "tg_usuarios_insert"
        );
        for (String name : triggers) {
            try {
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + name);
                log.debug("Trigger '{}' eliminado (si existía).", name);
            } catch (Exception ex) {
                log.warn("No se pudo eliminar trigger '{}': {}", name, ex.getMessage());
            }
        }
        // Búsqueda dinámica de triggers sobre la tabla usuarios
        try {
            List<String> found = jdbcTemplate.queryForList(
                "SELECT TRIGGER_NAME FROM INFORMATION_SCHEMA.TRIGGERS " +
                "WHERE EVENT_OBJECT_TABLE = 'usuarios' AND TRIGGER_SCHEMA = DATABASE()",
                String.class
            );
            for (String triggerName : found) {
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS `" + triggerName + "`");
                log.info("Trigger dinámico '{}' eliminado.", triggerName);
            }
        } catch (Exception ex) {
            log.warn("No se pudo consultar INFORMATION_SCHEMA: {}", ex.getMessage());
        }
    }

    private void seedRoles() {
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
    }

    private void seedAdminUser() {
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

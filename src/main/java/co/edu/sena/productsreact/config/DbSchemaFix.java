package co.edu.sena.productsreact.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajusta las columnas de perfiles_usuario a nullable con DEFAULT ''
 * para que el trigger de la tabla usuarios no falle al insertar.
 * Se ejecuta una sola vez al arrancar la aplicación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DbSchemaFix {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void fixSchema() {
        String[] alters = {
            "ALTER TABLE perfiles_usuario MODIFY COLUMN nombres VARCHAR(100) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN apellidos VARCHAR(100) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN tipo_identificacion VARCHAR(30) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN identificacion VARCHAR(20) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN telefono VARCHAR(20) NULL DEFAULT ''",
            "ALTER TABLE perfiles_usuario MODIFY COLUMN direccion VARCHAR(200) NULL DEFAULT ''"
        };

        for (String sql : alters) {
            try {
                jdbcTemplate.execute(sql);
                log.info("[DbSchemaFix] OK: {}", sql);
            } catch (Exception e) {
                log.error("[DbSchemaFix] FALLÓ: {} — {}", sql, e.getMessage());
            }
        }

        // Intentar eliminar triggers conocidos en tabla usuarios
        String[] triggers = {
            "after_insert_usuario", "after_insert_usuarios",
            "trg_crear_perfil", "trg_crear_perfil_usuario",
            "crear_perfil_usuario", "crear_perfil",
            "trg_perfil_usuario", "trg_perfil",
            "after_usuario_insert", "after_usuarios_insert",
            "tg_usuarios_insert", "tg_usuario_insert",
            "ins_perfil_usuario", "ins_usuarios",
            "trigger_usuarios", "trigger_usuario"
        };

        for (String name : triggers) {
            try {
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + name);
            } catch (Exception ignored) {}
        }

        // Búsqueda dinámica de todos los triggers sobre la tabla usuarios
        try {
            jdbcTemplate.queryForList(
                "SELECT TRIGGER_NAME FROM INFORMATION_SCHEMA.TRIGGERS " +
                "WHERE EVENT_OBJECT_TABLE = 'usuarios' AND TRIGGER_SCHEMA = DATABASE()",
                String.class
            ).forEach(name -> {
                try {
                    jdbcTemplate.execute("DROP TRIGGER IF EXISTS `" + name + "`");
                    log.info("[DbSchemaFix] Trigger eliminado: {}", name);
                } catch (Exception e) {
                    log.error("[DbSchemaFix] No se pudo eliminar trigger {}: {}", name, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("[DbSchemaFix] No se pudo consultar INFORMATION_SCHEMA: {}", e.getMessage());
        }
    }
}

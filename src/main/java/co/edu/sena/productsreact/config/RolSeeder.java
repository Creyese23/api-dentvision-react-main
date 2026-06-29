package co.edu.sena.productsreact.config;

// Entidad Rol que se insertará en la tabla "roles" de la BD
import co.edu.sena.productsreact.entity.Rol;
// Repositorio para acceder y guardar roles en la BD
import co.edu.sena.productsreact.repository.RolRepository;
// Lombok: genera el constructor con los campos final (inyección por constructor)
import lombok.RequiredArgsConstructor;
// @Slf4j: genera automáticamente un logger estático (log.info, log.error, etc.)
import lombok.extern.slf4j.Slf4j;
// ApplicationArguments: parámetros de línea de comandos recibidos al iniciar la app (puede estar vacío)
import org.springframework.boot.ApplicationArguments;
// ApplicationRunner: interfaz que ejecuta el método run() una vez que Spring Boot termina de arrancar
import org.springframework.boot.ApplicationRunner;
// @Component: declara esta clase como componente de Spring para que sea detectada y ejecutada automáticamente
import org.springframework.stereotype.Component;

// List: para definir la lista inmutable de roles que deben existir en la BD
import java.util.List;

// @Slf4j: inyecta un logger estático para registrar mensajes en consola/archivo de log
@Slf4j
// @Component: Spring detecta y ejecuta este bean al arrancar la aplicación
@Component
// @RequiredArgsConstructor: genera el constructor con rolRepository como parámetro
@RequiredArgsConstructor
// Implementa ApplicationRunner: el método run() se ejecuta automáticamente después del arranque de Spring Boot
public class RolSeeder implements ApplicationRunner {

    // Lista de todos los roles que deben existir en la tabla "roles" de la BD
    // Se usa List.of() para crear una lista inmutable (no se puede modificar en tiempo de ejecución)
    // Spring Security requiere el prefijo "ROLE_" en todos los nombres de roles
    private static final List<String> ROLES = List.of(
            "ROLE_ADMIN",          // Administrador del sistema: acceso total
            "ROLE_ODONTOLOGO",     // Odontólogo: gestiona citas y procedimientos de sus pacientes
            "ROLE_RECEPCIONISTA",  // Recepcionista: agenda citas y gestiona pacientes
            "ROLE_TECNICO",        // Técnico dental: ejecuta procedimientos y gestiona inventario
            "ROLE_PACIENTE"        // Paciente: solo puede ver sus propias citas y datos
    );

    // Repositorio para buscar y guardar roles en la tabla "roles" de la BD
    private final RolRepository rolRepository;

    // Método ejecutado automáticamente una vez que Spring Boot termina de arrancar y la BD está lista
    // Garantiza que los roles necesarios existan antes de que cualquier usuario intente registrarse
    @Override
    public void run(ApplicationArguments args) {
        // Itera sobre cada rol definido en la lista ROLES
        for (String nombreRol : ROLES) {
            // Verifica si el rol ya existe en la BD antes de intentar crearlo (evita duplicados)
            if (rolRepository.findByNombreRol(nombreRol).isEmpty()) {
                // Crea y guarda el nuevo rol usando el patrón Builder de Lombok
                rolRepository.save(Rol.builder().nombreRol(nombreRol).build());
                // Registra en el log que el rol fue creado exitosamente (visible en consola al iniciar)
                log.info("Rol creado: {}", nombreRol);
            }
            // Si el rol ya existe, no hace nada (operación idempotente: segura de ejecutar múltiples veces)
        }
    }
}

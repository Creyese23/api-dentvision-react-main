package co.edu.sena.productsreact.config;

// Entidad Servicio que se insertará en la tabla "servicios" de la BD
import co.edu.sena.productsreact.entity.Servicio;
// Repositorio para acceder y guardar servicios odontológicos en la BD
import co.edu.sena.productsreact.repository.ServicioRepository;
// Lombok: genera el constructor con los campos final (inyección por constructor)
import lombok.RequiredArgsConstructor;
// @Slf4j: genera automáticamente un logger estático para registrar mensajes
import lombok.extern.slf4j.Slf4j;
// ApplicationArguments: parámetros de línea de comandos al iniciar (puede estar vacío)
import org.springframework.boot.ApplicationArguments;
// ApplicationRunner: interfaz cuyo método run() se ejecuta automáticamente al arrancar Spring Boot
import org.springframework.boot.ApplicationRunner;
// @Component: declara esta clase como componente de Spring (detectado y ejecutado automáticamente)
import org.springframework.stereotype.Component;
// @Transactional: agrupa todas las operaciones de BD en una sola transacción (todo o nada)
import org.springframework.transaction.annotation.Transactional;

// @Component: Spring detecta y ejecuta este bean después del arranque de la aplicación
@Component
// @RequiredArgsConstructor: genera el constructor con servicioRepository como parámetro
@RequiredArgsConstructor
// @Slf4j: inyecta un logger para registrar qué servicios se crean al iniciar
@Slf4j
// Implementa ApplicationRunner: el método run() se ejecuta una vez que el contexto está listo
public class DataInitializer implements ApplicationRunner {

    // Repositorio para verificar si los servicios ya existen y guardarlos en la BD
    private final ServicioRepository servicioRepository;

    // @Transactional: si alguna inserción falla, se revierten todas las inserciones de este método
    // Ejecutado automáticamente al arrancar la aplicación para poblar el catálogo de servicios
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Crea los servicios odontológicos base del catálogo si no existen en la BD
        // Cada llamada es idempotente: solo inserta si el nombre no existe previamente
        crearServicioSiNoExiste("Odontología General",
            "Consultas y tratamientos generales de odontología incluyendo diagnóstico, limpieza, obturaciones y cuidado preventivo.");
        crearServicioSiNoExiste("Ortodoncia",
            "Corrección de la posición de dientes y mandíbula mediante brackets, alineadores y otros dispositivos.");
        crearServicioSiNoExiste("Endodoncia",
            "Tratamiento del conducto radicular para eliminar infecciones y salvar dientes dañados.");
        crearServicioSiNoExiste("Periodoncia",
            "Tratamiento de encías y tejidos de soporte de los dientes.");
        crearServicioSiNoExiste("Cirugía Oral",
            "Extracciones, implantes y procedimientos quirúrgicos bucales.");
    }

    // Método auxiliar: crea y guarda el servicio solo si no existe en la BD
    // Recibe el nombre y descripción del servicio a crear
    private void crearServicioSiNoExiste(String nombre, String descripcion) {
        // Verifica si ya existe un servicio con ese nombre exacto en la tabla "servicios"
        if (!servicioRepository.existsByNombre(nombre)) {
            // Construye y guarda el nuevo servicio con el patrón Builder de Lombok
            servicioRepository.save(Servicio.builder()
                .nombre(nombre)           // Nombre del servicio odontológico
                .descripcion(descripcion) // Descripción detallada del servicio
                .estado(true)             // El servicio se crea activo y disponible
                .build());
            // Registra en el log que el servicio fue creado (visible en la consola al arrancar)
            log.info("[DataInitializer] Servicio creado: {}", nombre);
        }
        // Si ya existe, no hace nada (operación idempotente: segura de ejecutar múltiples veces)
    }
}

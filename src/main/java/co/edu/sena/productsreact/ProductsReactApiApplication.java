package co.edu.sena.productsreact;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductsReactApiApplication {

    public static void main(String[] args) {
        loadDotEnv();
        String envProfile = System.getProperty("SPRING_PROFILES_ACTIVE");
        if (envProfile != null && !envProfile.isBlank()
                && System.getProperty("spring.profiles.active") == null) {
            System.setProperty("spring.profiles.active", envProfile);
        }
        SpringApplication.run(ProductsReactApiApplication.class, args);
    }

    private static void loadDotEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> {
                // Solo seteamos si no existe ya en el entorno real.
                if (System.getProperty(entry.getKey()) == null
                        && System.getenv(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });
        } catch (Exception ex) {
            System.out.println("[dotenv] No se pudo cargar el archivo .env: " + ex.getMessage());
        }
    }
}

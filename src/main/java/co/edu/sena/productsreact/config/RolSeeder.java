package co.edu.sena.productsreact.config;

import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RolSeeder implements ApplicationRunner {

    private static final List<String> ROLES = List.of(
            "ROLE_ADMIN",
            "ROLE_ODONTOLOGO",
            "ROLE_RECEPCIONISTA",
            "ROLE_TECNICO",
            "ROLE_PACIENTE"
    );

    private final RolRepository rolRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (String nombreRol : ROLES) {
            if (rolRepository.findByNombreRol(nombreRol).isEmpty()) {
                rolRepository.save(Rol.builder().nombreRol(nombreRol).build());
                log.info("Rol creado: {}", nombreRol);
            }
        }
    }
}

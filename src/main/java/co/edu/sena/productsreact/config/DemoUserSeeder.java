package co.edu.sena.productsreact.config;

import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.repository.RolRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("mysql-dev")
@RequiredArgsConstructor
public class DemoUserSeeder implements ApplicationRunner {

    private static final List<DemoAccount> ACCOUNTS = List.of(
        new DemoAccount("admin@dentvision.local", "Admin123*", "ROLE_ADMIN"),
        new DemoAccount("odontologo@dentvision.local", "Odonto123*", "ROLE_ODONTOLOGO"),
        new DemoAccount("tecnico@dentvision.local", "Tecnico123*", "ROLE_TECNICO"),
        new DemoAccount("recepcion@dentvision.local", "Recep123*", "ROLE_RECEPCIONISTA"),
        new DemoAccount("paciente@dentvision.local", "Paciente123*", "ROLE_PACIENTE")
    );

    private final RolRepository rolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DemoAccount account : ACCOUNTS) {
            if (userRepository.existsByEmail(account.email())) {
                continue;
            }

            Rol rol = rolRepository.findByNombreRol(account.role())
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + account.role()));

            userRepository.save(User.builder()
                .email(account.email())
                .password(passwordEncoder.encode(account.password()))
                .provider("LOCAL")
                .estado(true)
                .emailVerified(true)
                .rol(rol)
                .build());
        }
    }

    private record DemoAccount(String email, String password, String role) {}
}

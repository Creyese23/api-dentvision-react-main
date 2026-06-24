package co.edu.sena.productsreact.config;

import co.edu.sena.productsreact.entity.Servicio;
import co.edu.sena.productsreact.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final ServicioRepository servicioRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
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

    private void crearServicioSiNoExiste(String nombre, String descripcion) {
        if (!servicioRepository.existsByNombre(nombre)) {
            servicioRepository.save(Servicio.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .estado(true)
                .build());
            log.info("[DataInitializer] Servicio creado: {}", nombre);
        }
    }
}

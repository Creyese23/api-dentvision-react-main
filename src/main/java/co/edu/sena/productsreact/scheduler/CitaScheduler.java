package co.edu.sena.productsreact.scheduler;

import co.edu.sena.productsreact.entity.Cita;
import co.edu.sena.productsreact.repository.CitaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CitaScheduler {

    private final CitaRepository citaRepository;

    /**
     * Cada hora revisa citas PROGRAMADA cuya fecha ya pasó y las marca como NO_ASISTIO.
     * Cron: segundos minutos horas día mes díaSemana
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void marcarCitasNoAsistidas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Cita> vencidas = citaRepository.findProgramadasVencidas(ahora);

        if (vencidas.isEmpty()) return;

        vencidas.forEach(c -> c.setEstado("NO_ASISTIO"));
        citaRepository.saveAll(vencidas);

        log.info("[CitaScheduler] {} cita(s) marcada(s) como NO_ASISTIO", vencidas.size());
    }
}

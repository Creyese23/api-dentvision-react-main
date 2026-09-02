package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.esterilizacion.EsterilizacionRequest;
import co.edu.sena.productsreact.dto.esterilizacion.EsterilizacionResponse;
import co.edu.sena.productsreact.entity.Cita;
import co.edu.sena.productsreact.entity.Esterilizacion;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.CitaRepository;
import co.edu.sena.productsreact.repository.EsterilizacionRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EsterilizacionService {

    private final EsterilizacionRepository esterilizacionRepository;
    private final CitaRepository citaRepository;
    private final UserRepository userRepository;

    /** Retorna el estado vigente de la cita; si aún no fue registrado, devuelve 404. */
    public EsterilizacionResponse obtenerPorCita(Long citaId) {
        return EsterilizacionResponse.from(esterilizacionRepository.findByCitaId(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay estado de esterilización para la cita " + citaId)));
    }

    /** Crea o actualiza el estado vigente y conserva quién lo reportó y cuándo. */
    @Transactional
    public EsterilizacionResponse guardarPorCita(Long citaId, EsterilizacionRequest request, String emailResponsable) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita " + citaId + " no encontrada"));
        User responsable = userRepository.findByEmail(emailResponsable)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario responsable no encontrado"));

        Esterilizacion esterilizacion = esterilizacionRepository.findByCitaId(citaId)
                .orElseGet(() -> Esterilizacion.builder().cita(cita).build());
        esterilizacion.setEstado(request.estado());
        esterilizacion.setObservaciones(request.observaciones());
        esterilizacion.setResponsable(responsable);
        esterilizacion.setFechaRegistro(LocalDateTime.now());
        return EsterilizacionResponse.from(esterilizacionRepository.save(esterilizacion));
    }
}

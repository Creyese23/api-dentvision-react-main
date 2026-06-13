package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.comunicacion.MensajeRequest;
import co.edu.sena.productsreact.dto.comunicacion.MensajeResponse;
import co.edu.sena.productsreact.entity.*;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ConversacionService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PacienteRepository pacienteRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long iniciarConversacion(Long pacienteId, Long procedimientoId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + pacienteId + " no encontrado"));
        Procedimiento proc = null;
        if (procedimientoId != null) {
            proc = procedimientoRepository.findById(procedimientoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + procedimientoId + " no encontrado"));
        }
        Conversacion c = Conversacion.builder()
                .paciente(paciente).procedimiento(proc)
                .inicio(LocalDateTime.now()).build();
        return conversacionRepository.save(c).getId();
    }

    public List<MensajeResponse> mensajes(Long conversacionId) {
        return mensajeRepository.findByConversacionIdOrderByHoraAsc(conversacionId)
                .stream().map(MensajeResponse::from).toList();
    }

    @Transactional
    public MensajeResponse enviarMensaje(MensajeRequest req) {
        Conversacion conv = conversacionRepository.findById(req.idConversacion())
                .orElseThrow(() -> new ResourceNotFoundException("Conversación " + req.idConversacion() + " no encontrada"));
        User remitente = userRepository.findById(req.idRemitente())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + req.idRemitente() + " no encontrado"));

        Mensaje m = Mensaje.builder()
                .conversacion(conv).remitente(remitente)
                .contenido(req.contenido())
                .hora(LocalDateTime.now())
                .estado("ENVIADO").build();
        return MensajeResponse.from(mensajeRepository.save(m));
    }

    @Transactional
    public void cerrarConversacion(Long id) {
        Conversacion c = conversacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación " + id + " no encontrada"));
        c.setFin(LocalDateTime.now());
        conversacionRepository.save(c);
    }
}

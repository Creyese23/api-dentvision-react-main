package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.entrega.EntregaResponse;
import co.edu.sena.productsreact.entity.Entrega;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.EntregaRepository;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final UserRepository userRepository;
    private final PacienteRepository pacienteRepository;

    public List<EntregaResponse> listar() {
        return entregaRepository.findAll().stream().map(EntregaResponse::from).toList();
    }

    public Optional<EntregaResponse> obtenerPorOrden(Long ordenId) {
        return entregaRepository.findByOrdenId(ordenId).map(EntregaResponse::from);
    }

    @Transactional
    public EntregaResponse cambiarEstado(Long id, String estado) {
        Entrega e = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega no encontrada"));
        e.setEstado(estado);
        if ("ENTREGADO".equals(estado)) e.setFechaEntrega(LocalDateTime.now());
        return EntregaResponse.from(entregaRepository.save(e));
    }

    public List<EntregaResponse> listarMisEntregas(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        return entregaRepository.findByPacienteId(paciente.getId())
                .stream().map(EntregaResponse::from).toList();
    }
}

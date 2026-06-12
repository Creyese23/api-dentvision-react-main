package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.paciente.PacienteRequest;
import co.edu.sena.productsreact.dto.paciente.PacienteResponse;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UserRepository userRepository;

    public List<PacienteResponse> listarActivos() {
        return pacienteRepository.findByEstadoTrue().stream()
                .map(PacienteResponse::from).toList();
    }

    public Page<PacienteResponse> buscar(String q, Pageable pageable) {
        return pacienteRepository.buscar(q, pageable).map(PacienteResponse::from);
    }

    public PacienteResponse obtener(Long id) {
        return PacienteResponse.from(getOrThrow(id));
    }

    @Transactional
    public PacienteResponse crear(PacienteRequest req) {
        if (pacienteRepository.existsByDocumento(req.documento())) {
            throw new DuplicateResourceException("Ya existe un paciente con documento " + req.documento());
        }
        Paciente p = Paciente.builder()
                .nombres(req.nombres()).apellidos(req.apellidos())
                .documento(req.documento()).tipoIdentificacion(req.tipoIdentificacion())
                .telefono(req.telefono()).genero(req.genero())
                .fechaNacimiento(req.fechaNacimiento()).estado(true)
                .build();
        if (req.idUsuario() != null) {
            User u = userRepository.findById(req.idUsuario())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario " + req.idUsuario() + " no encontrado"));
            p.setUsuario(u);
        }
        return PacienteResponse.from(pacienteRepository.save(p));
    }

    @Transactional
    public PacienteResponse actualizar(Long id, PacienteRequest req) {
        Paciente p = getOrThrow(id);
        p.setNombres(req.nombres());
        p.setApellidos(req.apellidos());
        p.setTipoIdentificacion(req.tipoIdentificacion());
        p.setTelefono(req.telefono());
        p.setGenero(req.genero());
        p.setFechaNacimiento(req.fechaNacimiento());
        return PacienteResponse.from(pacienteRepository.save(p));
    }

    @Transactional
    public void eliminar(Long id) {
        Paciente p = getOrThrow(id);
        p.setEstado(false);
        p.setFechaEliminacion(LocalDateTime.now());
        pacienteRepository.save(p);
    }

    private Paciente getOrThrow(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + id + " no encontrado"));
    }
}

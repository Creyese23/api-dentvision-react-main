package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.paciente.PacienteRequest;
import co.edu.sena.productsreact.dto.paciente.PacienteResponse;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
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
    private final PerfilUsuarioRepository perfilRepository;

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
        User usuario = userRepository.findById(req.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + req.idUsuario() + " no encontrado"));

        if (pacienteRepository.existsByUsuarioId(req.idUsuario())) {
            throw new DuplicateResourceException("Ya existe un paciente vinculado al usuario " + req.idUsuario());
        }
        if (perfilRepository.existsByIdentificacion(req.identificacion())) {
            throw new DuplicateResourceException("Ya existe un perfil con identificación " + req.identificacion());
        }

        PerfilUsuario perfil = usuario.getPerfil();
        if (perfil == null) {
            perfil = new PerfilUsuario();
            perfil.setUsuario(usuario);
        }
        perfil.setNombres(req.nombres());
        perfil.setApellidos(req.apellidos());
        perfil.setIdentificacion(req.identificacion());
        perfil.setTipoIdentificacion(req.tipoIdentificacion());
        perfil.setTelefono(req.telefono());
        perfil.setDireccion(req.direccion());
        perfilRepository.save(perfil);

        Paciente p = Paciente.builder()
                .usuario(usuario)
                .fechaNacimiento(req.fechaNacimiento())
                .estado(true)
                .build();
        return PacienteResponse.from(pacienteRepository.save(p));
    }

    @Transactional
    public PacienteResponse actualizar(Long id, PacienteRequest req) {
        Paciente p = getOrThrow(id);
        p.setFechaNacimiento(req.fechaNacimiento());

        if (p.getUsuario() != null) {
            PerfilUsuario perfil = p.getUsuario().getPerfil();
            if (perfil == null) {
                perfil = new PerfilUsuario();
                perfil.setUsuario(p.getUsuario());
            }
            perfil.setNombres(req.nombres());
            perfil.setApellidos(req.apellidos());
            perfil.setIdentificacion(req.identificacion());
            perfil.setTipoIdentificacion(req.tipoIdentificacion());
            perfil.setTelefono(req.telefono());
            perfil.setDireccion(req.direccion());
            perfilRepository.save(perfil);
        }
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

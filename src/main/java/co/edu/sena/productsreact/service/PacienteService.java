package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.paciente.PacienteRequest;
import co.edu.sena.productsreact.dto.paciente.PacienteResponse;
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;
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
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UserRepository userRepository;
    private final PerfilUsuarioRepository perfilRepository;

    public List<PacienteResponse> listarActivos() {
        return pacienteRepository.findByEstadoTrueAndFechaEliminacionIsNull().stream()
                .map(PacienteResponse::from).toList();
    }

    public Page<PacienteResponse> buscar(String q, Pageable pageable) {
        return pacienteRepository.buscar(q, pageable).map(PacienteResponse::from);
    }

    public PacienteResponse obtener(Long id) {
        return PacienteResponse.from(getOrThrow(id));
    }

    public PacienteResponse obtenerMiPaciente(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente p = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes un perfil de paciente registrado"));
        return PacienteResponse.from(p);
    }

    public List<UsuarioResponse> listarPendientesActivacion() {
        return userRepository.findPacientesSinActivar().stream()
                .map(UsuarioResponse::from).toList();
    }

    @Transactional
    public PacienteResponse activarPaciente(Long idUsuario) {
        User user = userRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + idUsuario + " no encontrado"));
        if (pacienteRepository.existsByUsuarioId(idUsuario)) {
            throw new DuplicateResourceException("El usuario ya tiene un perfil de paciente activo");
        }
        Paciente p = Paciente.builder()
                .usuario(user)
                .estado(true)
                .build();
        return PacienteResponse.from(pacienteRepository.save(p));
    }

    @Transactional
    public PacienteResponse crear(PacienteRequest req) {
        User usuario = userRepository.findById(req.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + req.idUsuario() + " no encontrado"));

        if (pacienteRepository.existsByUsuarioId(req.idUsuario())) {
            throw new DuplicateResourceException("Ya existe un paciente vinculado al usuario " + req.idUsuario());
        }
        if (req.identificacion() != null && !req.identificacion().isBlank()
                && perfilRepository.existsByIdentificacion(req.identificacion())) {
            throw new DuplicateResourceException("Ya existe un perfil con identificación " + req.identificacion());
        }

        PerfilUsuario perfil = usuario.getPerfil();
        if (perfil == null) {
            perfil = new PerfilUsuario();
            perfil.setUsuario(usuario);
        }
        perfil.setNombres(req.nombres() != null ? req.nombres() : "");
        perfil.setApellidos(req.apellidos() != null ? req.apellidos() : "");
        perfil.setIdentificacion(req.identificacion() != null ? req.identificacion() : "");
        perfil.setTipoIdentificacion(req.tipoIdentificacion() != null ? req.tipoIdentificacion() : "");
        perfil.setTelefono(req.telefono() != null ? req.telefono() : "");
        perfil.setDireccion(req.direccion() != null ? req.direccion() : "");
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
            perfil.setNombres(req.nombres() != null ? req.nombres() : "");
            perfil.setApellidos(req.apellidos() != null ? req.apellidos() : "");
            perfil.setIdentificacion(req.identificacion() != null ? req.identificacion() : "");
            perfil.setTipoIdentificacion(req.tipoIdentificacion() != null ? req.tipoIdentificacion() : "");
            perfil.setTelefono(req.telefono() != null ? req.telefono() : "");
            perfil.setDireccion(req.direccion() != null ? req.direccion() : "");
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

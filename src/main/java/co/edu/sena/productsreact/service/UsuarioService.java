package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.usuario.CreateUsuarioRequest;
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
import co.edu.sena.productsreact.repository.RefreshTokenRepository;
import co.edu.sena.productsreact.repository.RolRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final PacienteRepository pacienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponse> listar() {
        return userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEstado()))
                .map(UsuarioResponse::from).toList();
    }

    public List<UsuarioResponse> listarTodos() {
        return userRepository.findAll().stream()
                .map(UsuarioResponse::from).toList();
    }

    @Transactional
    public UsuarioResponse toggleEstado(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " no encontrado"));
        user.setEstado(!Boolean.TRUE.equals(user.getEstado()));
        return UsuarioResponse.from(userRepository.save(user));
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, String nuevoRolNombre) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " no encontrado"));

        Rol rol = rolRepository.findByNombreRol(nuevoRolNombre)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol(nuevoRolNombre).build()));
        user.setRol(rol);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUsuarioId(id);

        if ("ROLE_PACIENTE".equals(nuevoRolNombre)) {
            // Reactivar o crear registro en pacientes
            pacienteRepository.findByUsuarioId(id).ifPresentOrElse(p -> {
                p.setEstado(true);
                p.setFechaEliminacion(null);
                pacienteRepository.save(p);
            }, () -> pacienteRepository.save(Paciente.builder().usuario(user).estado(true).build()));

            // Desactivar empleado si existe
            empleadoRepository.findByUsuarioId(id).ifPresent(e -> {
                if (Boolean.TRUE.equals(e.getEstado())) {
                    e.setEstado(false);
                    e.setFechaEliminacion(LocalDateTime.now());
                    empleadoRepository.save(e);
                }
            });
        } else {
            // Desactivar registro en pacientes si existe
            pacienteRepository.findByUsuarioId(id).ifPresent(p -> {
                if (Boolean.TRUE.equals(p.getEstado())) {
                    p.setEstado(false);
                    p.setFechaEliminacion(LocalDateTime.now());
                    pacienteRepository.save(p);
                }
            });
        }

        return UsuarioResponse.from(user);
    }

    @Transactional
    public UsuarioResponse crear(CreateUsuarioRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("El email '" + req.email() + "' ya está registrado");
        }
        Rol rol = rolRepository.findByNombreRol(req.rol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol '" + req.rol() + "' no encontrado"));

        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .rol(rol)
                .estado(true)
                .build();
        user = userRepository.save(user);

        perfilRepository.save(PerfilUsuario.builder()
                .usuario(user)
                .nombres("").apellidos("")
                .tipoIdentificacion("").identificacion("")
                .telefono("").direccion("")
                .build());

        return UsuarioResponse.from(user);
    }
}

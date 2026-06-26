package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.empleado.EmpleadoRequest;
import co.edu.sena.productsreact.dto.empleado.EmpleadoResponse;
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

import java.util.Map;
import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
import co.edu.sena.productsreact.repository.RefreshTokenRepository;
import co.edu.sena.productsreact.repository.RolRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final UserRepository userRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final PacienteRepository pacienteRepository;
    private final RolRepository rolRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final Map<String, String> CARGO_ROL = Map.of(
            "ODONTOLOGO",    "ROLE_ODONTOLOGO",
            "TECNICO",       "ROLE_TECNICO",
            "RECEPCIONISTA", "ROLE_RECEPCIONISTA",
            "ADMINISTRADOR", "ROLE_ADMIN"
    );

    private void actualizarRolUsuario(User usuario, String cargo) {
        String nombreRol = CARGO_ROL.get(cargo);
        if (nombreRol == null) return;
        if (usuario.getRol() != null && nombreRol.equals(usuario.getRol().getNombreRol())) return;
        Rol rol = rolRepository.findByNombreRol(nombreRol)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol(nombreRol).build()));
        usuario.setRol(rol);
        userRepository.save(usuario);
        refreshTokenRepository.revokeAllByUsuarioId(usuario.getId());
        // Desactivar registro de paciente al convertirse en empleado
        pacienteRepository.findByUsuarioId(usuario.getId()).ifPresent(p -> {
            if (Boolean.TRUE.equals(p.getEstado())) {
                p.setEstado(false);
                p.setFechaEliminacion(java.time.LocalDateTime.now());
                pacienteRepository.save(p);
            }
        });
    }

    public List<EmpleadoResponse> listarActivos() {
        return empleadoRepository.findByEstadoTrueAndFechaEliminacionIsNull().stream()
                .map(EmpleadoResponse::from).toList();
    }

    public List<EmpleadoResponse> listarPorCargo(String cargo) {
        return empleadoRepository.findByCargoAndEstadoTrueAndFechaEliminacionIsNull(cargo).stream()
                .map(EmpleadoResponse::from).toList();
    }

    public EmpleadoResponse obtener(Long id) {
        return EmpleadoResponse.from(getOrThrow(id));
    }

    public EmpleadoResponse obtenerMiEmpleado(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Empleado e = empleadoRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes un perfil de empleado registrado"));
        return EmpleadoResponse.from(e);
    }

    private static final Map<String, String> ROL_CARGO = Map.of(
            "ROLE_ODONTOLOGO",    "ODONTOLOGO",
            "ROLE_TECNICO",       "TECNICO",
            "ROLE_RECEPCIONISTA", "RECEPCIONISTA",
            "ROLE_ADMIN",         "ADMINISTRADOR"
    );

    public List<UsuarioResponse> listarPendientesActivacion() {
        List<String> roles = List.of("ROLE_ODONTOLOGO", "ROLE_TECNICO", "ROLE_RECEPCIONISTA", "ROLE_ADMIN");
        return userRepository.findByRolNombreRolInAndEstadoTrue(roles).stream()
                .filter(u -> !empleadoRepository.existsByUsuarioId(u.getId()))
                .map(UsuarioResponse::from).toList();
    }

    @Transactional
    public EmpleadoResponse activarEmpleado(Long idUsuario) {
        User user = userRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + idUsuario + " no encontrado"));
        if (empleadoRepository.existsByUsuarioId(idUsuario)) {
            throw new DuplicateResourceException("El usuario ya tiene un perfil de empleado activo");
        }
        String cargo = ROL_CARGO.getOrDefault(user.getRol().getNombreRol(), "EMPLEADO");
        Empleado e = Empleado.builder()
                .usuario(user)
                .cargo(cargo)
                .estado(true)
                .build();
        return EmpleadoResponse.from(empleadoRepository.save(e));
    }

    @Transactional
    public EmpleadoResponse crear(EmpleadoRequest req) {
        User usuario = userRepository.findById(req.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + req.idUsuario() + " no encontrado"));

        if (empleadoRepository.existsByUsuarioId(req.idUsuario())) {
            throw new DuplicateResourceException("Ya existe un empleado vinculado al usuario " + req.idUsuario());
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

        actualizarRolUsuario(usuario, req.cargo());

        Empleado e = Empleado.builder()
                .usuario(usuario)
                .cargo(req.cargo())
                .genero(req.genero())
                .especialidad(req.especialidad())
                .estado(true)
                .build();
        return EmpleadoResponse.from(empleadoRepository.save(e));
    }

    @Transactional
    public EmpleadoResponse actualizarProfesional(String email, String especialidad) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Empleado e = empleadoRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes un perfil de empleado registrado"));
        e.setEspecialidad(especialidad);
        return EmpleadoResponse.from(empleadoRepository.save(e));
    }

    @Transactional
    public EmpleadoResponse actualizar(Long id, EmpleadoRequest req) {
        Empleado e = getOrThrow(id);
        e.setCargo(req.cargo());
        e.setGenero(req.genero());
        e.setEspecialidad(req.especialidad());

        if (e.getUsuario() != null) {
            actualizarRolUsuario(e.getUsuario(), req.cargo());
            PerfilUsuario perfil = e.getUsuario().getPerfil();
            if (perfil == null) {
                perfil = new PerfilUsuario();
                perfil.setUsuario(e.getUsuario());
            }
            perfil.setNombres(req.nombres());
            perfil.setApellidos(req.apellidos());
            perfil.setIdentificacion(req.identificacion());
            perfil.setTipoIdentificacion(req.tipoIdentificacion());
            perfil.setTelefono(req.telefono());
            perfil.setDireccion(req.direccion());
            perfilRepository.save(perfil);
        }
        return EmpleadoResponse.from(empleadoRepository.save(e));
    }

    @Transactional
    public void eliminar(Long id) {
        Empleado e = getOrThrow(id);
        e.setEstado(false);
        e.setFechaEliminacion(LocalDateTime.now());
        empleadoRepository.save(e);

        // Revertir rol a ROLE_PACIENTE, reactivar paciente y forzar re-login
        User usuario = e.getUsuario();
        if (usuario != null) {
            Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                    .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol("ROLE_PACIENTE").build()));
            usuario.setRol(rolPaciente);
            userRepository.save(usuario);
            refreshTokenRepository.revokeAllByUsuarioId(usuario.getId());
            // Reactivar o crear registro de paciente
            pacienteRepository.findByUsuarioId(usuario.getId()).ifPresentOrElse(p -> {
                p.setEstado(true);
                p.setFechaEliminacion(null);
                pacienteRepository.save(p);
            }, () -> pacienteRepository.save(Paciente.builder().usuario(usuario).estado(true).build()));
        }
    }

    private Empleado getOrThrow(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado " + id + " no encontrado"));
    }
}

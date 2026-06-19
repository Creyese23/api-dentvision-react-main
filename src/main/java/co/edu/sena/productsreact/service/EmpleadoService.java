package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.empleado.EmpleadoRequest;
import co.edu.sena.productsreact.dto.empleado.EmpleadoResponse;
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

import java.util.Map;
import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
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
    private final RolRepository rolRepository;

    private static final Map<String, String> CARGO_ROL = Map.of(
            "ODONTOLOGO",    "ROLE_ODONTOLOGO",
            "TECNICO",       "ROLE_TECNICO",
            "RECEPCIONISTA", "ROLE_RECEPCIONISTA",
            "ADMINISTRADOR", "ROLE_ADMIN",
            "AUXILIAR",      "ROLE_TECNICO"
    );

    private void actualizarRolUsuario(User usuario, String cargo) {
        String nombreRol = CARGO_ROL.get(cargo);
        if (nombreRol == null) return;
        if (usuario.getRol() != null && nombreRol.equals(usuario.getRol().getNombreRol())) return;
        Rol rol = rolRepository.findByNombreRol(nombreRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol '" + nombreRol + "' no encontrado"));
        usuario.setRol(rol);
        userRepository.save(usuario);
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
                .estado(true)
                .build();
        return EmpleadoResponse.from(empleadoRepository.save(e));
    }

    @Transactional
    public EmpleadoResponse actualizar(Long id, EmpleadoRequest req) {
        Empleado e = getOrThrow(id);
        e.setCargo(req.cargo());

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
    }

    private Empleado getOrThrow(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado " + id + " no encontrado"));
    }
}

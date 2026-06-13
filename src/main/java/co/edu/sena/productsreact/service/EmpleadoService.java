package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.empleado.EmpleadoRequest;
import co.edu.sena.productsreact.dto.empleado.EmpleadoResponse;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final UserRepository userRepository;
    private final PerfilUsuarioRepository perfilRepository;

    public List<EmpleadoResponse> listarActivos() {
        return empleadoRepository.findByEstadoTrue().stream()
                .map(EmpleadoResponse::from).toList();
    }

    public List<EmpleadoResponse> listarPorCargo(String cargo) {
        return empleadoRepository.findByCargo(cargo).stream()
                .map(EmpleadoResponse::from).toList();
    }

    public EmpleadoResponse obtener(Long id) {
        return EmpleadoResponse.from(getOrThrow(id));
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

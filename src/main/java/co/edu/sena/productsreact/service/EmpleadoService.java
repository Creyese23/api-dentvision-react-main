package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.empleado.EmpleadoRequest;
import co.edu.sena.productsreact.dto.empleado.EmpleadoResponse;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
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
        if (empleadoRepository.existsByDocumento(req.documento())) {
            throw new DuplicateResourceException("Ya existe un empleado con documento " + req.documento());
        }
        Empleado e = Empleado.builder()
                .nombres(req.nombres()).apellidos(req.apellidos())
                .documento(req.documento()).tipoIdentificacion(req.tipoIdentificacion())
                .telefono(req.telefono()).cargo(req.cargo()).estado(true)
                .build();
        if (req.idUsuario() != null) {
            User u = userRepository.findById(req.idUsuario())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario " + req.idUsuario() + " no encontrado"));
            e.setUsuario(u);
        }
        return EmpleadoResponse.from(empleadoRepository.save(e));
    }

    @Transactional
    public EmpleadoResponse actualizar(Long id, EmpleadoRequest req) {
        Empleado e = getOrThrow(id);
        e.setNombres(req.nombres());
        e.setApellidos(req.apellidos());
        e.setTipoIdentificacion(req.tipoIdentificacion());
        e.setTelefono(req.telefono());
        e.setCargo(req.cargo());
        return EmpleadoResponse.from(empleadoRepository.save(e));
    }

    @Transactional
    public void eliminar(Long id) {
        Empleado e = getOrThrow(id);
        e.setEstado(false);
        e.setFechaEliminacion(LocalDateTime.now());
        empleadoRepository.save(e);
    }

    public Empleado getOrThrow(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado " + id + " no encontrado"));
    }
}

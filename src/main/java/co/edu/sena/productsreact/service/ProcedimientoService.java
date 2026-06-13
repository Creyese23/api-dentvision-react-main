package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoRequest;
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoResponse;
import co.edu.sena.productsreact.entity.Cita;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Procedimiento;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.repository.CitaRepository;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.ProcedimientoRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ProcedimientoService {

    private final ProcedimientoRepository procedimientoRepository;
    private final CitaRepository citaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UserRepository userRepository;
    private final PacienteRepository pacienteRepository;

    public List<ProcedimientoResponse> listarMiHistorial(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        return procedimientoRepository.findByCitaPacienteIdAndFechaEliminacionIsNull(paciente.getId())
                .stream().map(ProcedimientoResponse::from).toList();
    }

    public List<ProcedimientoResponse> listarPorTecnicoAutenticado(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Empleado tecnico = empleadoRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));
        return procedimientoRepository.findByTecnicoIdAndFechaEliminacionIsNull(tecnico.getId())
                .stream().map(ProcedimientoResponse::from).toList();
    }

    public List<ProcedimientoResponse> listarPorCita(Long citaId) {
        return procedimientoRepository.findByCitaIdAndFechaEliminacionIsNull(citaId)
                .stream().map(ProcedimientoResponse::from).toList();
    }

    public ProcedimientoResponse obtener(Long id) {
        return ProcedimientoResponse.from(getOrThrow(id));
    }

    @Transactional
    public ProcedimientoResponse crear(ProcedimientoRequest req) {
        Cita cita = citaRepository.findById(req.idCita())
                .orElseThrow(() -> new ResourceNotFoundException("Cita " + req.idCita() + " no encontrada"));

        Empleado tecnico = null;
        if (req.idTecnico() != null) {
            tecnico = empleadoRepository.findById(req.idTecnico())
                    .orElseThrow(() -> new ResourceNotFoundException("Técnico " + req.idTecnico() + " no encontrado"));
        }

        Procedimiento p = Procedimiento.builder()
                .cita(cita).tecnico(tecnico)
                .nombre(req.nombre()).descripcion(req.descripcion())
                .costo(req.costo()).estado("EN_PROCESO")
                .fechaInicio(req.fechaInicio()).fechaFin(req.fechaFin())
                .observaciones(req.observaciones())
                .build();
        return ProcedimientoResponse.from(procedimientoRepository.save(p));
    }

    @Transactional
    public ProcedimientoResponse actualizar(Long id, ProcedimientoRequest req) {
        Procedimiento p = getOrThrow(id);
        Empleado tecnico = null;
        if (req.idTecnico() != null) {
            tecnico = empleadoRepository.findById(req.idTecnico())
                    .orElseThrow(() -> new ResourceNotFoundException("Técnico " + req.idTecnico() + " no encontrado"));
        }
        p.setTecnico(tecnico);
        p.setNombre(req.nombre());
        p.setDescripcion(req.descripcion());
        p.setCosto(req.costo());
        p.setFechaInicio(req.fechaInicio());
        p.setFechaFin(req.fechaFin());
        p.setObservaciones(req.observaciones());
        return ProcedimientoResponse.from(procedimientoRepository.save(p));
    }

    @Transactional
    public ProcedimientoResponse cambiarEstado(Long id, String estado) {
        Procedimiento p = getOrThrow(id);
        p.setEstado(estado);
        if ("COMPLETADO".equals(estado) && p.getFechaFin() == null) {
            p.setFechaFin(LocalDateTime.now());
        }
        return ProcedimientoResponse.from(procedimientoRepository.save(p));
    }

    @Transactional
    public void eliminar(Long id) {
        Procedimiento p = getOrThrow(id);
        p.setFechaEliminacion(LocalDateTime.now());
        procedimientoRepository.save(p);
    }

    public Procedimiento getOrThrow(Long id) {
        return procedimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + id + " no encontrado"));
    }
}

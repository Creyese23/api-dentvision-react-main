package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoInsumoResponse;
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoRequest;
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoResponse;
import co.edu.sena.productsreact.entity.*;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@Slf4j
public class ProcedimientoService {

    private final ProcedimientoRepository procedimientoRepository;
    private final CitaRepository citaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UserRepository userRepository;
    private final PacienteRepository pacienteRepository;
    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
    private final EmailService emailService;
    @Lazy private final FacturaService facturaService;

    public List<ProcedimientoResponse> listarMiHistorial(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        return procedimientoRepository.findByCitaPacienteIdWithDetails(paciente.getId())
                .stream().map(ProcedimientoResponse::from).toList();
    }

    public List<ProcedimientoResponse> listarPorTecnicoAutenticado(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return empleadoRepository.findByUsuarioId(user.getId())
                .map(tecnico -> procedimientoRepository.findByTecnicoIdWithDetails(tecnico.getId())
                        .stream().map(ProcedimientoResponse::from).toList())
                .orElse(java.util.List.of());
    }

    public List<ProcedimientoResponse> listarPorCita(Long citaId) {
        return procedimientoRepository.findByCitaIdWithDetails(citaId)
                .stream().map(ProcedimientoResponse::from).toList();
    }

    public ProcedimientoResponse obtener(Long id) {
        Procedimiento p = procedimientoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + id + " no encontrado"));
        return ProcedimientoResponse.from(p);
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
        ProcedimientoResponse resultado = ProcedimientoResponse.from(procedimientoRepository.save(p));

        // Notificar al paciente sobre el cambio de estado
        try {
            String email = p.getCita().getPaciente().getUsuario().getEmail();
            String nombre = p.getCita().getPaciente().getUsuario().getPerfil().getNombres();
            emailService.sendProcedimientoActualizado(email, nombre, p.getNombre(), estado);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de procedimiento: {}", e.getMessage());
        }

        // Auto-generar factura cuando todos los procedimientos de la cita están COMPLETADO
        if ("COMPLETADO".equals(estado)) {
            Long citaId = p.getCita().getId();
            List<Procedimiento> todos = procedimientoRepository.findByCitaIdAndFechaEliminacionIsNull(citaId);
            boolean todosCompletos = !todos.isEmpty() && todos.stream().allMatch(pr -> "COMPLETADO".equals(pr.getEstado()));
            if (todosCompletos) {
                facturaService.crearDesdeCita(citaId);
            }
        }

        return resultado;
    }

    @Transactional
    public void eliminar(Long id) {
        Procedimiento p = getOrThrow(id);
        p.setFechaEliminacion(LocalDateTime.now());
        procedimientoRepository.save(p);
    }

    // ── Insumos por procedimiento ─────────────────────────────────────────────

    public List<ProcedimientoInsumoResponse> listarInsumos(Long procedimientoId) {
        getOrThrow(procedimientoId); // valida existencia
        return procedimientoInsumoRepository.findByProcedimientoId(procedimientoId)
                .stream().map(ProcedimientoInsumoResponse::from).toList();
    }

    @Transactional
    public ProcedimientoInsumoResponse registrarInsumo(Long procedimientoId, Long insumoId, Integer cantidad) {
        Procedimiento procedimiento = getOrThrow(procedimientoId);
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo " + insumoId + " no encontrado"));

        if (insumo.getStockActual() < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente. Disponible: " + insumo.getStockActual()
                    + " " + (insumo.getUnidadMedida() != null ? insumo.getUnidadMedida() : ""));
        }

        insumo.setStockActual(insumo.getStockActual() - cantidad);
        insumoRepository.save(insumo);

        movimientoRepository.save(MovimientoInventario.builder()
                .insumo(insumo).cantidad(cantidad).tipoMovimiento("SALIDA")
                .build());

        ProcedimientoInsumo pi = ProcedimientoInsumo.builder()
                .procedimiento(procedimiento).insumo(insumo).cantidad(cantidad)
                .build();
        return ProcedimientoInsumoResponse.from(procedimientoInsumoRepository.save(pi));
    }

    @Transactional
    public void eliminarInsumo(Long procedimientoInsumoId) {
        ProcedimientoInsumo pi = procedimientoInsumoRepository.findById(procedimientoInsumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de insumo no encontrado"));

        Insumo insumo = pi.getInsumo();
        insumo.setStockActual(insumo.getStockActual() + pi.getCantidad());
        insumoRepository.save(insumo);

        movimientoRepository.save(MovimientoInventario.builder()
                .insumo(insumo).cantidad(pi.getCantidad()).tipoMovimiento("ENTRADA")
                .build());

        procedimientoInsumoRepository.delete(pi);
    }

    public Procedimiento getOrThrow(Long id) {
        return procedimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + id + " no encontrado"));
    }
}

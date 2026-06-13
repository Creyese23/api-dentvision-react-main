package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.orden.OrdenRequest;
import co.edu.sena.productsreact.dto.orden.OrdenResponse;
import co.edu.sena.productsreact.entity.Entrega;
import co.edu.sena.productsreact.entity.Orden;
import co.edu.sena.productsreact.entity.Procedimiento;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.EntregaRepository;
import co.edu.sena.productsreact.repository.OrdenRepository;
import co.edu.sena.productsreact.repository.ProcedimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final EntregaRepository entregaRepository;

    public List<OrdenResponse> listarPorEstado(String estado) {
        return ordenRepository.findByEstado(estado).stream().map(OrdenResponse::from).toList();
    }

    public List<OrdenResponse> listarPorProcedimiento(Long procedimientoId) {
        return ordenRepository.findByProcedimientoId(procedimientoId)
                .stream().map(OrdenResponse::from).toList();
    }

    public OrdenResponse obtener(Long id) {
        return OrdenResponse.from(getOrThrow(id));
    }

    @Transactional
    public OrdenResponse crear(OrdenRequest req) {
        Procedimiento procedimiento = procedimientoRepository.findById(req.idProcedimiento())
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + req.idProcedimiento() + " no encontrado"));
        Orden orden = Orden.builder()
                .procedimiento(procedimiento)
                .descripcion(req.descripcion())
                .estado("PENDIENTE").build();
        Orden saved = ordenRepository.save(orden);

        Entrega entrega = Entrega.builder().orden(saved).estado("PENDIENTE").build();
        entregaRepository.save(entrega);

        return OrdenResponse.from(saved);
    }

    @Transactional
    public OrdenResponse cambiarEstado(Long id, String estado) {
        Orden o = getOrThrow(id);
        o.setEstado(estado);
        if ("ENTREGADA".equals(estado)) {
            entregaRepository.findByOrdenId(id).ifPresent(e -> {
                e.setEstado("ENTREGADA");
                e.setFechaEntrega(LocalDateTime.now());
                entregaRepository.save(e);
            });
        }
        return OrdenResponse.from(ordenRepository.save(o));
    }

    private Orden getOrThrow(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden " + id + " no encontrada"));
    }
}

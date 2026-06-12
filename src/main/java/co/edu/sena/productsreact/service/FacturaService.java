package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.factura.DetalleFacturaRequest;
import co.edu.sena.productsreact.dto.factura.FacturaRequest;
import co.edu.sena.productsreact.dto.factura.FacturaResponse;
import co.edu.sena.productsreact.entity.*;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final DetalleFacturaRepository detalleRepository;
    private final PacienteRepository pacienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServicioRepository servicioRepository;

    public List<FacturaResponse> listarPorPaciente(Long pacienteId) {
        return facturaRepository.findByPacienteIdAndFechaEliminacionIsNull(pacienteId)
                .stream().map(FacturaResponse::from).toList();
    }

    public FacturaResponse obtener(Long id) {
        return FacturaResponse.from(getOrThrow(id));
    }

    @Transactional
    public FacturaResponse crear(FacturaRequest req) {
        Paciente paciente = pacienteRepository.findById(req.idPaciente())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + req.idPaciente() + " no encontrado"));
        Empleado odontologo = null;
        if (req.idOdontologo() != null) {
            odontologo = empleadoRepository.findById(req.idOdontologo())
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado " + req.idOdontologo() + " no encontrado"));
        }
        Factura factura = Factura.builder()
                .paciente(paciente).odontologo(odontologo)
                .fechaEmision(LocalDateTime.now())
                .total(BigDecimal.ZERO).estado("PENDIENTE")
                .build();
        return FacturaResponse.from(facturaRepository.save(factura));
    }

    @Transactional
    public FacturaResponse agregarDetalle(Long facturaId, DetalleFacturaRequest req) {
        Factura factura = getOrThrow(facturaId);
        Servicio servicio = servicioRepository.findById(req.idServicio())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio " + req.idServicio() + " no encontrado"));

        BigDecimal precioUnitario = servicio.getPrecio();
        BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(req.cantidad()));

        DetalleFactura detalle = DetalleFactura.builder()
                .factura(factura).servicio(servicio)
                .cantidad(req.cantidad())
                .precioUnitario(precioUnitario)
                .subtotal(subtotal)
                .build();
        detalleRepository.save(detalle);

        recalcularTotal(factura);
        return FacturaResponse.from(facturaRepository.save(factura));
    }

    @Transactional
    public FacturaResponse cambiarEstado(Long id, String estado) {
        Factura f = getOrThrow(id);
        f.setEstado(estado);
        return FacturaResponse.from(facturaRepository.save(f));
    }

    @Transactional
    public void eliminar(Long id) {
        Factura f = getOrThrow(id);
        f.setFechaEliminacion(LocalDateTime.now());
        facturaRepository.save(f);
    }

    private void recalcularTotal(Factura factura) {
        BigDecimal total = detalleRepository.findByFacturaId(factura.getId())
                .stream().map(DetalleFactura::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        factura.setTotal(total);
    }

    private Factura getOrThrow(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura " + id + " no encontrada"));
    }
}

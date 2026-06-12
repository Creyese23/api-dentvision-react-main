package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.pago.PagoRequest;
import co.edu.sena.productsreact.dto.pago.PagoResponse;
import co.edu.sena.productsreact.entity.Factura;
import co.edu.sena.productsreact.entity.Pago;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.FacturaRepository;
import co.edu.sena.productsreact.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;

    public List<PagoResponse> listarPorFactura(Long facturaId) {
        return pagoRepository.findByFacturaIdAndFechaEliminacionIsNull(facturaId)
                .stream().map(PagoResponse::from).toList();
    }

    @Transactional
    public PagoResponse registrar(PagoRequest req) {
        Factura factura = facturaRepository.findById(req.idFactura())
                .orElseThrow(() -> new ResourceNotFoundException("Factura " + req.idFactura() + " no encontrada"));

        Pago pago = Pago.builder()
                .factura(factura).valor(req.valor())
                .metodoPago(req.metodoPago()).estado("COMPLETADO")
                .fechaPago(req.fechaPago() != null ? req.fechaPago() : LocalDateTime.now())
                .build();
        Pago saved = pagoRepository.save(pago);

        BigDecimal totalPagado = pagoRepository.sumValorByFacturaId(factura.getId());
        if (totalPagado.compareTo(factura.getTotal()) >= 0) {
            factura.setEstado("PAGADA");
            facturaRepository.save(factura);
        }

        return PagoResponse.from(saved);
    }

    @Transactional
    public void anular(Long id) {
        Pago p = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago " + id + " no encontrado"));
        p.setEstado("ANULADO");
        p.setFechaEliminacion(LocalDateTime.now());
        pagoRepository.save(p);
    }
}

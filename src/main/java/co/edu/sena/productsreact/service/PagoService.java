package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.pago.PagoRequest;
import co.edu.sena.productsreact.dto.pago.PagoResponse;
import co.edu.sena.productsreact.entity.Factura;
import co.edu.sena.productsreact.entity.Pago;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.FacturaRepository;
import co.edu.sena.productsreact.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@Slf4j
public class PagoService {

    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;
    private final EmailService emailService;

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
        boolean facturaCompleta = totalPagado.compareTo(factura.getTotal()) >= 0;
        if (facturaCompleta) {
            factura.setEstado("PAGADA");
            facturaRepository.save(factura);
        }

        try {
            String email = factura.getPaciente().getUsuario().getEmail();
            String nombre = factura.getPaciente().getUsuario().getPerfil().getNombres();
            String valor = String.format("%,.0f", req.valor()).replace(",", ".");
            emailService.sendPagoConfirmado(email, nombre, factura.getId(), valor, req.metodoPago(), facturaCompleta);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de pago: {}", e.getMessage());
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

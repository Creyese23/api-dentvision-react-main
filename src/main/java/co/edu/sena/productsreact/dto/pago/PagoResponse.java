package co.edu.sena.productsreact.dto.pago;

import co.edu.sena.productsreact.entity.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(
        Long id,
        Long idFactura,
        BigDecimal valor,
        String metodoPago,
        String estado,
        LocalDateTime fechaPago
) {
    public static PagoResponse from(Pago p) {
        return new PagoResponse(
                p.getId(), p.getFactura().getId(),
                p.getValor(), p.getMetodoPago(),
                p.getEstado(), p.getFechaPago()
        );
    }
}

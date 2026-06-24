package co.edu.sena.productsreact.dto.factura;

import co.edu.sena.productsreact.entity.DetalleFactura;

import java.math.BigDecimal;

public record DetalleFacturaResponse(
        Long id,
        String concepto,
        String servicio,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static DetalleFacturaResponse from(DetalleFactura d) {
        return new DetalleFacturaResponse(
                d.getId(),
                d.getConcepto() != null ? d.getConcepto()
                        : (d.getServicio() != null ? d.getServicio().getNombre() : "Servicio"),
                d.getServicio() != null ? d.getServicio().getNombre() : null,
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getSubtotal()
        );
    }
}

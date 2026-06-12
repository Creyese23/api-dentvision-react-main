package co.edu.sena.productsreact.dto.insumo;

import co.edu.sena.productsreact.entity.Insumo;

import java.math.BigDecimal;

public record InsumoResponse(
        Long id,
        String nombre,
        String descripcion,
        String unidadMedida,
        Integer stockActual,
        Integer stockMinimo,
        BigDecimal precioUnitario,
        Boolean estado,
        Boolean bajoPedido
) {
    public static InsumoResponse from(Insumo i) {
        return new InsumoResponse(
                i.getId(), i.getNombre(), i.getDescripcion(),
                i.getUnidadMedida(), i.getStockActual(), i.getStockMinimo(),
                i.getPrecioUnitario(), i.getEstado(),
                i.getStockActual() <= i.getStockMinimo()
        );
    }
}

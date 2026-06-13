package co.edu.sena.productsreact.dto.entrega;

import co.edu.sena.productsreact.entity.Entrega;

import java.time.LocalDateTime;

public record EntregaResponse(
        Long id,
        Long idOrden,
        String descripcionOrden,
        String estadoOrden,
        String estado,
        LocalDateTime fechaEntrega,
        String observaciones
) {
    public static EntregaResponse from(Entrega e) {
        return new EntregaResponse(
                e.getId(),
                e.getOrden().getId(),
                e.getOrden().getDescripcion(),
                e.getOrden().getEstado(),
                e.getEstado(),
                e.getFechaEntrega(),
                e.getObservaciones()
        );
    }
}

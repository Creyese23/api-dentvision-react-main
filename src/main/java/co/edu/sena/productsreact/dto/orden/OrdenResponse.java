package co.edu.sena.productsreact.dto.orden;

import co.edu.sena.productsreact.entity.Orden;

import java.time.LocalDateTime;

public record OrdenResponse(
        Long id,
        Long idProcedimiento,
        String descripcion,
        String estado,
        LocalDateTime fechaCreacion,
        String imagenDiagnostica
) {
    public static OrdenResponse from(Orden o) {
        return new OrdenResponse(
                o.getId(), o.getProcedimiento().getId(),
                o.getDescripcion(), o.getEstado(), o.getFechaCreacion(),
                o.getImagenDiagnostica()
        );
    }
}

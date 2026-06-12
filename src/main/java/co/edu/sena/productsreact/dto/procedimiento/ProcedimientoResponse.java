package co.edu.sena.productsreact.dto.procedimiento;

import co.edu.sena.productsreact.entity.Procedimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProcedimientoResponse(
        Long id,
        Long idCita,
        Long idTecnico,
        String nombreTecnico,
        String nombre,
        String descripcion,
        BigDecimal costo,
        String estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String observaciones
) {
    public static ProcedimientoResponse from(Procedimiento p) {
        String tecnico = p.getTecnico() != null
                ? p.getTecnico().getNombres() + " " + p.getTecnico().getApellidos()
                : null;
        return new ProcedimientoResponse(
                p.getId(), p.getCita().getId(),
                p.getTecnico() != null ? p.getTecnico().getId() : null,
                tecnico, p.getNombre(), p.getDescripcion(),
                p.getCosto(), p.getEstado(),
                p.getFechaInicio(), p.getFechaFin(), p.getObservaciones()
        );
    }
}

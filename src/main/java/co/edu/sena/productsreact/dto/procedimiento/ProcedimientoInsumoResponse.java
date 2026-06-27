package co.edu.sena.productsreact.dto.procedimiento;

import co.edu.sena.productsreact.entity.ProcedimientoInsumo;

public record ProcedimientoInsumoResponse(
        Long id,
        Long idInsumo,
        String nombreInsumo,
        String unidadMedida,
        Integer cantidad
) {
    public static ProcedimientoInsumoResponse from(ProcedimientoInsumo pi) {
        return new ProcedimientoInsumoResponse(
                pi.getId(),
                pi.getInsumo().getId(),
                pi.getInsumo().getNombre(),
                pi.getInsumo().getUnidadMedida(),
                pi.getCantidad()
        );
    }
}

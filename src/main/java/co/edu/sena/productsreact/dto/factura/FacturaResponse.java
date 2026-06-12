package co.edu.sena.productsreact.dto.factura;

import co.edu.sena.productsreact.entity.Factura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FacturaResponse(
        Long id,
        Long idPaciente,
        String nombrePaciente,
        Long idOdontologo,
        String nombreOdontologo,
        LocalDateTime fechaEmision,
        BigDecimal total,
        String estado
) {
    public static FacturaResponse from(Factura f) {
        String paciente = f.getPaciente().getNombres() + " " + f.getPaciente().getApellidos();
        String odontologo = f.getOdontologo() != null
                ? f.getOdontologo().getNombres() + " " + f.getOdontologo().getApellidos()
                : null;
        return new FacturaResponse(
                f.getId(), f.getPaciente().getId(), paciente,
                f.getOdontologo() != null ? f.getOdontologo().getId() : null, odontologo,
                f.getFechaEmision(), f.getTotal(), f.getEstado()
        );
    }
}

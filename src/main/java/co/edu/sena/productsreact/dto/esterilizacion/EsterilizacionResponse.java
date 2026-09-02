package co.edu.sena.productsreact.dto.esterilizacion;

import co.edu.sena.productsreact.entity.Esterilizacion;
import java.time.LocalDateTime;

public record EsterilizacionResponse(
        Long id,
        Long idCita,
        String estado,
        String observaciones,
        Long idResponsable,
        String responsable,
        LocalDateTime fechaRegistro
) {
    public static EsterilizacionResponse from(Esterilizacion esterilizacion) {
        var responsable = esterilizacion.getResponsable();
        var perfil = responsable.getPerfil();
        String nombre = perfil == null
                ? responsable.getEmail()
                : (perfil.getNombres() + " " + perfil.getApellidos()).trim();
        return new EsterilizacionResponse(
                esterilizacion.getId(),
                esterilizacion.getCita().getId(),
                esterilizacion.getEstado().name(),
                esterilizacion.getObservaciones(),
                responsable.getId(),
                nombre,
                esterilizacion.getFechaRegistro()
        );
    }
}

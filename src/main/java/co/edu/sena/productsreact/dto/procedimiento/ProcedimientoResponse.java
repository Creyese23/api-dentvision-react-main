package co.edu.sena.productsreact.dto.procedimiento;

import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.Procedimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProcedimientoResponse(
        Long id,
        Long idCita,
        Long idTecnico,
        String nombreTecnico,
        String nombreOdontologo,
        String nombre,
        String descripcion,
        BigDecimal costo,
        String estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String observaciones
) {
    public static ProcedimientoResponse from(Procedimiento p) {
        Empleado odontologo = p.getCita() != null ? p.getCita().getOdontologo() : null;
        return new ProcedimientoResponse(
                p.getId(), p.getCita().getId(),
                p.getTecnico() != null ? p.getTecnico().getId() : null,
                p.getTecnico() != null ? nombreCompletoEmpleado(p.getTecnico()) : null,
                odontologo != null ? nombreCompletoEmpleado(odontologo) : null,
                p.getNombre(), p.getDescripcion(),
                p.getCosto(), p.getEstado(),
                p.getFechaInicio(), p.getFechaFin(), p.getObservaciones()
        );
    }

    private static String nombreCompletoEmpleado(Empleado e) {
        PerfilUsuario perfil = e.getUsuario() != null ? e.getUsuario().getPerfil() : null;
        if (perfil == null) return "";
        return perfil.getNombres() + " " + perfil.getApellidos();
    }
}

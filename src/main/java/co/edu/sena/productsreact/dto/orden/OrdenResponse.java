package co.edu.sena.productsreact.dto.orden;

import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Orden;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;

import java.time.LocalDateTime;

public record OrdenResponse(
        Long id,
        Long idProcedimiento,
        String descripcion,
        String color,
        String estado,
        LocalDateTime fechaCreacion,
        String imagenDiagnostica,
        String odontograma,
        String nombreOdontologo,
        String nombrePaciente
) {
    public static OrdenResponse from(Orden o) {
        Paciente paciente = o.getProcedimiento().getCita().getPaciente();
        Empleado odontologo = o.getProcedimiento().getCita().getOdontologo();
        return new OrdenResponse(
                o.getId(), o.getProcedimiento().getId(),
                o.getDescripcion(), o.getColor(), o.getEstado(), o.getFechaCreacion(),
                o.getImagenDiagnostica(), o.getOdontograma(),
                nombreCompletoEmpleado(odontologo), nombreCompleto(paciente)
        );
    }

    private static String nombreCompleto(Paciente p) {
        PerfilUsuario perfil = p.getUsuario() != null ? p.getUsuario().getPerfil() : null;
        if (perfil == null) return "";
        return perfil.getNombres() + " " + perfil.getApellidos();
    }

    private static String nombreCompletoEmpleado(Empleado e) {
        PerfilUsuario perfil = e.getUsuario() != null ? e.getUsuario().getPerfil() : null;
        if (perfil == null) return "";
        return perfil.getNombres() + " " + perfil.getApellidos();
    }
}

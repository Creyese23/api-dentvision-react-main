package co.edu.sena.productsreact.dto.cita;

import co.edu.sena.productsreact.entity.Cita;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;

import java.time.LocalDateTime;

public record CitaResponse(
        Long id,
        Long idPaciente,
        String nombrePaciente,
        Long idOdontologo,
        String nombreOdontologo,
        LocalDateTime fechaHora,
        String motivo,
        String estado
) {
    public static CitaResponse from(Cita c) {
        return new CitaResponse(
                c.getId(),
                c.getPaciente().getId(), nombreCompleto(c.getPaciente()),
                c.getOdontologo().getId(), nombreCompletoEmpleado(c.getOdontologo()),
                c.getFechaHora(), c.getMotivo(), c.getEstado()
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

package co.edu.sena.productsreact.dto.empleado;

import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.PerfilUsuario;

import java.time.LocalDateTime;

public record EmpleadoResponse(
        Long id,
        Long idUsuario,
        String nombres,
        String apellidos,
        String identificacion,
        String tipoIdentificacion,
        String telefono,
        String cargo,
        Boolean estado,
        LocalDateTime fechaCreacion
) {
    public static EmpleadoResponse from(Empleado e) {
        PerfilUsuario perfil = e.getUsuario() != null ? e.getUsuario().getPerfil() : null;
        return new EmpleadoResponse(
                e.getId(),
                e.getUsuario() != null ? e.getUsuario().getId() : null,
                perfil != null ? perfil.getNombres() : null,
                perfil != null ? perfil.getApellidos() : null,
                perfil != null ? perfil.getIdentificacion() : null,
                perfil != null ? perfil.getTipoIdentificacion() : null,
                perfil != null ? perfil.getTelefono() : null,
                e.getCargo(),
                e.getEstado(),
                e.getFechaCreacion()
        );
    }
}

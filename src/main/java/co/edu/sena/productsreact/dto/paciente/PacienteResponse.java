package co.edu.sena.productsreact.dto.paciente;

import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PacienteResponse(
        Long id,
        Long idUsuario,
        String nombres,
        String apellidos,
        String identificacion,
        String tipoIdentificacion,
        String telefono,
        String direccion,
        LocalDate fechaNacimiento,
        Boolean estado,
        LocalDateTime fechaCreacion
) {
    public static PacienteResponse from(Paciente p) {
        PerfilUsuario perfil = p.getUsuario() != null ? p.getUsuario().getPerfil() : null;
        return new PacienteResponse(
                p.getId(),
                p.getUsuario() != null ? p.getUsuario().getId() : null,
                perfil != null ? perfil.getNombres() : null,
                perfil != null ? perfil.getApellidos() : null,
                perfil != null ? perfil.getIdentificacion() : null,
                perfil != null ? perfil.getTipoIdentificacion() : null,
                perfil != null ? perfil.getTelefono() : null,
                perfil != null ? perfil.getDireccion() : null,
                p.getFechaNacimiento(),
                p.getEstado(),
                p.getFechaCreacion()
        );
    }
}

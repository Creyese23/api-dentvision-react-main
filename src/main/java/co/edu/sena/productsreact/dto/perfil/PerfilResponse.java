package co.edu.sena.productsreact.dto.perfil;

import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.User;

import java.time.LocalDate;

public record PerfilResponse(
        Long idUsuario,
        String email,
        String rol,
        String nombres,
        String apellidos,
        String tipoIdentificacion,
        String identificacion,
        String telefono,
        String direccion,
        String fotoPerfil,
        LocalDate fechaNacimiento
) {
    public static PerfilResponse from(User user, PerfilUsuario p) {
        return new PerfilResponse(
                user.getId(), user.getEmail(), user.getRol().getNombreRol(),
                p.getNombres(), p.getApellidos(),
                p.getTipoIdentificacion(), p.getIdentificacion(),
                p.getTelefono(), p.getDireccion(),
                p.getFotoPerfil(),
                null
        );
    }

    public static PerfilResponse from(User user, PerfilUsuario p, Paciente paciente) {
        return new PerfilResponse(
                user.getId(), user.getEmail(), user.getRol().getNombreRol(),
                p.getNombres(), p.getApellidos(),
                p.getTipoIdentificacion(), p.getIdentificacion(),
                p.getTelefono(), p.getDireccion(),
                p.getFotoPerfil(),
                paciente != null ? paciente.getFechaNacimiento() : null
        );
    }
}

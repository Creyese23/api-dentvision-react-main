package co.edu.sena.productsreact.dto.perfil;

import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.User;

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
        String fotoPerfil
) {
    public static PerfilResponse from(User user, PerfilUsuario p) {
        return new PerfilResponse(
                user.getId(), user.getEmail(), user.getRol().getNombreRol(),
                p.getNombres(), p.getApellidos(),
                p.getTipoIdentificacion(), p.getIdentificacion(),
                p.getTelefono(), p.getDireccion(),
                p.getFotoPerfil()
        );
    }
}

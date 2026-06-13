package co.edu.sena.productsreact.dto.usuario;

import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.User;

public record UsuarioResponse(
        Long id,
        String email,
        String rol,
        boolean estado,
        String nombres,
        String apellidos
) {
    public static UsuarioResponse from(User u) {
        PerfilUsuario p = u.getPerfil();
        return new UsuarioResponse(
                u.getId(), u.getEmail(), u.getRol().getNombreRol(), u.getEstado(),
                p != null ? p.getNombres() : null,
                p != null ? p.getApellidos() : null
        );
    }
}

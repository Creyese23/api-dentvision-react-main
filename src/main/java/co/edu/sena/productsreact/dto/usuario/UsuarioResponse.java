package co.edu.sena.productsreact.dto.usuario;

import co.edu.sena.productsreact.entity.User;

public record UsuarioResponse(
        Long id,
        String email,
        String rol,
        boolean estado
) {
    public static UsuarioResponse from(User u) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getRol().getNombreRol(), u.getEstado());
    }
}

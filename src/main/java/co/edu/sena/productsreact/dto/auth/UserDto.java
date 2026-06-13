package co.edu.sena.productsreact.dto.auth;

public record UserDto(
        Long id,
        String email,
        String rol,
        boolean estado,
        String nombres,
        String apellidos
) {}

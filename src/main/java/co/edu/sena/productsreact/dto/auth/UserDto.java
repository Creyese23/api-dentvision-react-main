package co.edu.sena.productsreact.dto.auth;

public record UserDto(
        Long id,
        String email,
        String rol,
        String password,
        boolean estado
        /*String nombres,
        String apellidos*/
) {}

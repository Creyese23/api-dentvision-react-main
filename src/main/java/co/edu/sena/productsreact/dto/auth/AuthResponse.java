package co.edu.sena.productsreact.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {}

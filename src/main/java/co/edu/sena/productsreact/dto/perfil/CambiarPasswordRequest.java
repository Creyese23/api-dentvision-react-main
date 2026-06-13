package co.edu.sena.productsreact.dto.perfil;

public record CambiarPasswordRequest(
        String passwordActual,
        String passwordNuevo
) {}

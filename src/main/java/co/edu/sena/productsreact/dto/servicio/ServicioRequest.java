package co.edu.sena.productsreact.dto.servicio;

import jakarta.validation.constraints.NotBlank;

public record ServicioRequest(
        @NotBlank String nombre,
        String descripcion,
        String imagenUrl,
        java.math.BigDecimal precio
) {}

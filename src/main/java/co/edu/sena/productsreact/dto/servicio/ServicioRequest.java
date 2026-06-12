package co.edu.sena.productsreact.dto.servicio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ServicioRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @DecimalMin("0.0") BigDecimal precio,
        Integer duracionMinutos
) {}

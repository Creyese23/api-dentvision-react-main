package co.edu.sena.productsreact.dto.servicio;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record SubServicioRequest(
        @NotBlank String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos
) {}

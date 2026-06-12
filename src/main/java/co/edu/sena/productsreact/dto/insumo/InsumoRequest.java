package co.edu.sena.productsreact.dto.insumo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record InsumoRequest(
        @NotBlank String nombre,
        String descripcion,
        String unidadMedida,
        @Min(0) Integer stockActual,
        @Min(0) Integer stockMinimo,
        BigDecimal precioUnitario
) {}

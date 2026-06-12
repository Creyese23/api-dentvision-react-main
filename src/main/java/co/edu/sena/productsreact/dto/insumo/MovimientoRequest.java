package co.edu.sena.productsreact.dto.insumo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MovimientoRequest(
        @NotNull Long idInsumo,
        Long idEmpleado,
        @NotNull @Min(1) Integer cantidad,
        @NotBlank String tipoMovimiento
) {}

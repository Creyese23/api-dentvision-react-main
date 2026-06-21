package co.edu.sena.productsreact.dto.factura;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetalleFacturaRequest(
        @NotNull Long idServicio,
        Long idSubServicio,
        @NotNull @Min(1) Integer cantidad
) {}

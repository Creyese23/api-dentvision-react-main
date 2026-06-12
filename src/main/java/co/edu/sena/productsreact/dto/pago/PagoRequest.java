package co.edu.sena.productsreact.dto.pago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoRequest(
        @NotNull Long idFactura,
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        @NotBlank String metodoPago,
        LocalDateTime fechaPago
) {}

package co.edu.sena.productsreact.dto.factura;

import jakarta.validation.constraints.NotNull;

public record FacturaRequest(
        @NotNull Long idPaciente,
        Long idOdontologo
) {}

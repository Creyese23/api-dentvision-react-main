package co.edu.sena.productsreact.dto.orden;

import jakarta.validation.constraints.NotNull;

public record OrdenRequest(
        @NotNull Long idProcedimiento,
        String descripcion,
        String color,
        String odontograma
) {}

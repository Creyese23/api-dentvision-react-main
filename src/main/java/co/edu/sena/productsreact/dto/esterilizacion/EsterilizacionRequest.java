package co.edu.sena.productsreact.dto.esterilizacion;

import co.edu.sena.productsreact.entity.EstadoEsterilizacion;
import jakarta.validation.constraints.NotNull;

public record EsterilizacionRequest(
        @NotNull EstadoEsterilizacion estado,
        String observaciones
) {}

package co.edu.sena.productsreact.dto.procedimiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProcedimientoRequest(
        @NotNull Long idCita,
        Long idTecnico,
        @NotBlank String nombre,
        String descripcion,
        BigDecimal costo,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String observaciones
) {}

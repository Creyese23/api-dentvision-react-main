package co.edu.sena.productsreact.dto.cita;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CitaRequest(
        @NotNull Long idPaciente,
        @NotNull Long idOdontologo,
        @NotNull LocalDateTime fechaHora,
        String motivo
) {}

package co.edu.sena.productsreact.dto.cita;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendarCitaRequest(
        @NotNull Long idOdontologo,
        @NotNull LocalDateTime fechaHora,
        String motivo
) {}

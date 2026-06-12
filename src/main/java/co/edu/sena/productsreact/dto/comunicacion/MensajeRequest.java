package co.edu.sena.productsreact.dto.comunicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MensajeRequest(
        @NotNull Long idConversacion,
        @NotNull Long idRemitente,
        @NotBlank String contenido
) {}

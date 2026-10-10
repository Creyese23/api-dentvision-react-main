package co.edu.sena.productsreact.dto.orden;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrdenArchivoDiagnosticoRequest(
        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "[A-Za-z0-9_-]+")
        String id,
        @NotBlank
        @Size(max = 180)
        String name,
        @NotBlank
        @Pattern(regexp = "model/(stl|ply|obj)")
        String mimeType,
        @NotBlank
        @Size(max = 14_000_000)
        String base64
) {
}

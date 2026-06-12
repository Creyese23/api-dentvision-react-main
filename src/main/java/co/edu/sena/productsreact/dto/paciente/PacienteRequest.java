package co.edu.sena.productsreact.dto.paciente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotBlank @Size(max = 20) String documento,
        @NotBlank String tipoIdentificacion,
        String telefono,
        String genero,
        LocalDate fechaNacimiento,
        Long idUsuario
) {}

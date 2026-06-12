package co.edu.sena.productsreact.dto.empleado;

import jakarta.validation.constraints.NotBlank;

public record EmpleadoRequest(
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotBlank String documento,
        @NotBlank String tipoIdentificacion,
        String telefono,
        @NotBlank String cargo,
        Long idUsuario
) {}

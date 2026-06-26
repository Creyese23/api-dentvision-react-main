package co.edu.sena.productsreact.dto.empleado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmpleadoRequest(
        @NotNull(message = "El id de usuario es obligatorio")
        Long idUsuario,

        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotBlank @Size(max = 20) String identificacion,
        @NotBlank String tipoIdentificacion,
        String telefono,
        String direccion,
        @NotBlank String cargo,
        String genero,
        String especialidad
) {}

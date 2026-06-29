package co.edu.sena.productsreact.dto.auth;

// @Email: valida que el String tenga formato de correo electrónico válido
import jakarta.validation.constraints.Email;
// @NotBlank: valida que el String no sea null, vacío ni solo espacios en blanco
import jakarta.validation.constraints.NotBlank;
// @Size: valida el tamaño mínimo y máximo de un String
import jakarta.validation.constraints.Size;


// Record de Java (DTO de ENTRADA): transporta los datos necesarios para registrar un nuevo usuario
// Dirección: ENTRADA — el cliente envía este JSON en el body del POST /auth/register
// Ejemplo de JSON: {"email": "nuevo@example.com", "password": "miClaveSegura"}
// @Valid en el controlador activa las validaciones de los campos definidos aquí
public record RegisterRequest(
        // Campo email: dirección de correo que será el identificador único del nuevo usuario
        // @NotBlank: error si está vacío — "El email es obligatorio"
        // @Email: error si no tiene formato de email — "El email no tiene un formato válido"
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        // Campo password: contraseña en texto plano que será hasheada con BCrypt antes de guardarse
        // @NotBlank: error si está vacío — "La contraseña es obligatoria"
        // @Size(min=8, max=100): error si tiene menos de 8 o más de 100 caracteres
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        String password
) {}

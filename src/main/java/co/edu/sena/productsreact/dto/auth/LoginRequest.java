package co.edu.sena.productsreact.dto.auth;

// @Email: valida que el String tenga formato de email válido (ej: usuario@dominio.com)
import jakarta.validation.constraints.Email;
// @NotBlank: valida que el String no sea null, vacío ni solo espacios en blanco
import jakarta.validation.constraints.NotBlank;

// Record de Java (DTO de ENTRADA): transporta las credenciales del usuario desde el frontend hacia el servidor
// Dirección: ENTRADA — el cliente envía este JSON en el body del POST /auth/login
// Ejemplo de JSON: {"email": "juan@example.com", "password": "miClave123"}
// @Valid en el controlador activa las validaciones de los campos definidos aquí
public record LoginRequest(
        // Campo email: debe tener formato válido de correo electrónico
        // @NotBlank: error si está vacío — "El email es obligatorio"
        // @Email: error si no tiene formato email — "El email no tiene un formato válido"
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        // Campo password: contraseña en texto plano enviada por el usuario
        // El servicio la comparará contra el hash BCrypt almacenado en la BD
        // @NotBlank: error si está vacío — "La contraseña es obligatoria"
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}

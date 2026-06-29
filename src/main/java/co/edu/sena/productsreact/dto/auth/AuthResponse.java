package co.edu.sena.productsreact.dto.auth;

// Record de Java (DTO de SALIDA): transporta la respuesta de autenticación exitosa hacia el cliente
// Dirección: SALIDA — el servidor envía este JSON como respuesta del POST /auth/login o /auth/register
// Ejemplo de JSON:
// {
//   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
//   "refreshToken": "550e8400-e29b-41d4-a716-...",
//   "user": { "id": 1, "email": "juan@example.com", "rol": "ROLE_PACIENTE", ... }
// }
public record AuthResponse(
        // Token JWT de acceso de corta duración (ej: 1 hora)
        // El frontend debe incluirlo en el header "Authorization: Bearer <accessToken>" en cada petición
        String accessToken,

        // Token de renovación de larga duración (ej: 7 días)
        // El frontend lo envía al POST /auth/refresh para obtener un nuevo accessToken cuando el actual vence
        // Se almacena en la BD en la tabla "refresh_tokens" para poder revocarlo si es necesario
        String refreshToken,

        // Datos básicos del usuario autenticado para que el frontend pueda personalizar la UI
        // Incluye: id, email, rol, estado, nombres y apellidos
        UserDto user
) {}

package co.edu.sena.productsreact.dto.auth;

// Record de Java (DTO de SALIDA): transporta los datos públicos del usuario autenticado hacia el cliente
// Dirección: SALIDA — se incluye dentro de AuthResponse como parte de la respuesta de autenticación
// El frontend usa estos datos para mostrar el nombre del usuario, controlar rutas por rol, etc.
// Nunca incluye la contraseña ni datos sensibles (principio de mínimo privilegio)
public record UserDto(
        // Identificador único del usuario en la base de datos
        // El frontend lo usa para hacer peticiones específicas del usuario (ej: GET /api/pacientes/{userId})
        Long id,

        // Email del usuario (también es su nombre de usuario/login en el sistema)
        // Se muestra en la interfaz como identificador del usuario actual
        String email,

        // Nombre del rol del usuario (ej: "ROLE_ADMIN", "ROLE_PACIENTE", "ROLE_ODONTOLOGO")
        // El frontend lo usa para mostrar/ocultar secciones del menú según el rol
        String rol,

        // Estado de la cuenta: true = activa, false = deshabilitada
        // El frontend puede mostrar un aviso si la cuenta está deshabilitada
        boolean estado,

        // Nombres del usuario obtenidos de PerfilUsuario.nombres
        // Puede ser null si el usuario nunca completó su perfil (ej: primer login con OAuth2)
        String nombres,

        // Apellidos del usuario obtenidos de PerfilUsuario.apellidos
        // Puede ser null si el usuario nunca completó su perfil
        String apellidos
) {}

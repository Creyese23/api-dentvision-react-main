package co.edu.sena.productsreact.exception;

// LocalDateTime: tipo de dato para el timestamp del error (fecha y hora sin zona horaria)
import java.time.LocalDateTime;
// Map: mapa de campo -> mensaje de error, usado solo en errores de validación
import java.util.Map;

// Record de Java: clase inmutable con todos los campos como argumentos del constructor
// Se usa como DTO de SALIDA: es el cuerpo JSON que el servidor retorna cuando ocurre un error
// El frontend lo recibe y puede mostrar los mensajes de error al usuario
public record ApiError(
        // Código de estado HTTP numérico (ej: 400, 404, 409, 500)
        int status,
        // Nombre del error HTTP (ej: "Not Found", "Bad Request", "Conflict")
        String error,
        // Mensaje descriptivo del error específico (ej: "Usuario no encontrado con id: 5")
        String message,
        // URL de la petición que causó el error (ej: "/api/pacientes/5")
        String path,
        // Fecha y hora exacta en que ocurrió el error (para debugging y logs)
        LocalDateTime timestamp,
        // Mapa de errores de validación por campo: solo se usa en errores 400 de validación
        // Ejemplo: {"email": "formato inválido", "password": "muy corta"}
        // null en todos los otros tipos de error
        Map<String, String> fields
) {
    // Factory method estático: crea un ApiError para errores genéricos (404, 409, 500, etc.)
    // Agrega automáticamente el timestamp del momento en que se crea el objeto
    // El campo "fields" queda en null porque no es un error de validación de campos
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, LocalDateTime.now(), null);
    }

    // Factory method estático: crea un ApiError específico para errores de validación (400 Bad Request)
    // Incluye el mapa de campos con sus mensajes de error para que el frontend los muestre por campo
    // Fuerza el título a "Bad Request" ya que todos los errores de validación son 400
    public static ApiError ofValidation(int status, String message, String path, Map<String, String> fields) {
        return new ApiError(status, "Bad Request", message, path, LocalDateTime.now(), fields);
    }
}

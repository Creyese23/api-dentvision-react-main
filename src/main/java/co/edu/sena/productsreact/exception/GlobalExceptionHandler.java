package co.edu.sena.productsreact.exception;

// HttpServletRequest: petición HTTP entrante, usada para obtener la URL que causó el error
import jakarta.servlet.http.HttpServletRequest;
// HttpStatus: enumeración con los códigos de estado HTTP estándar (200, 400, 404, 500, etc.)
import org.springframework.http.HttpStatus;
// ResponseEntity: permite devolver una respuesta HTTP con cuerpo, headers y código de estado específico
import org.springframework.http.ResponseEntity;
// Excepción lanzada cuando el cuerpo JSON de la petición es inválido o está vacío
import org.springframework.http.converter.HttpMessageNotReadableException;
// Excepción de Spring Security cuando las credenciales (usuario/contraseña) son incorrectas
import org.springframework.security.authentication.BadCredentialsException;
// Excepción de Spring Security cuando el usuario existe pero su cuenta está deshabilitada
import org.springframework.security.authentication.DisabledException;
// Clase base de Spring Security para excepciones de autenticación
import org.springframework.security.core.AuthenticationException;
// FieldError: representa un error de validación en un campo específico del DTO
import org.springframework.validation.FieldError;
// Excepción lanzada por Spring cuando falla la validación de @Valid/@Validated en un DTO
import org.springframework.web.bind.MethodArgumentNotValidException;
// @ExceptionHandler: anotación que indica que el método maneja un tipo específico de excepción
import org.springframework.web.bind.annotation.ExceptionHandler;
// @RestControllerAdvice: intercepta excepciones de todos los controladores y retorna JSON
import org.springframework.web.bind.annotation.RestControllerAdvice;

// HashMap: mapa para acumular los errores de validación por campo
import java.util.HashMap;
// Map: interfaz del mapa de errores de validación
import java.util.Map;

// @RestControllerAdvice: Spring intercepta excepciones de cualquier @RestController de la app
// y las dirige al método @ExceptionHandler correspondiente para retornar una respuesta JSON uniforme
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Maneja ResourceNotFoundException: lanzada cuando se busca un recurso que no existe en la BD
    // Retorna HTTP 404 Not Found con el mensaje descriptivo de la excepción
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        // ApiError.of: crea el objeto de error con código 404, título "Not Found", mensaje y URL de la petición
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), req.getRequestURI())
        );
    }

    // Maneja DuplicateResourceException: lanzada cuando se intenta crear un recurso que ya existe
    // Retorna HTTP 409 Conflict (ej: intentar registrar un email que ya está en uso)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        // ApiError.of: crea el objeto de error con código 409, título "Conflict" y el mensaje
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), req.getRequestURI())
        );
    }

    // Maneja MethodArgumentNotValidException: lanzada cuando el DTO no pasa la validación de @Valid
    // Retorna HTTP 400 Bad Request con un mapa de errores por campo (ej: {"email": "formato inválido"})
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // Mapa para acumular los errores: clave = nombre del campo, valor = mensaje del error
        Map<String, String> fields = new HashMap<>();
        // Itera sobre todos los errores de campo del resultado de la validación
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Agrega cada par campo-mensaje al mapa (ej: "password" -> "debe tener entre 8 y 100 caracteres")
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        // ApiError.ofValidation: crea el error con el mapa de campos para que el frontend muestre cada error
        return ResponseEntity.badRequest().body(
                ApiError.ofValidation(HttpStatus.BAD_REQUEST.value(),
                        "Errores de validación en los datos enviados", req.getRequestURI(), fields)
        );
    }

    // Maneja HttpMessageNotReadableException: lanzada cuando el body de la petición no es JSON válido
    // Retorna HTTP 400 Bad Request con un mensaje indicando el problema con el cuerpo de la petición
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        "El cuerpo de la petición no es un JSON válido o está vacío",
                        req.getRequestURI())
        );
    }

    // Maneja IllegalArgumentException: lanzada manualmente en el código cuando un argumento es inválido
    // Retorna HTTP 400 Bad Request con el mensaje de la excepción
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), req.getRequestURI())
        );
    }

    // Maneja BadCredentialsException: lanzada por Spring Security cuando email o contraseña son incorrectos
    // Retorna HTTP 401 Unauthorized con un mensaje genérico (no revela si el email existe o no)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                        "Credenciales inválidas", req.getRequestURI())
        );
    }

    // Maneja DisabledException: lanzada cuando el usuario existe pero su cuenta está desactivada (estado = false)
    // Retorna HTTP 401 Unauthorized informando que la cuenta está deshabilitada
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                        "La cuenta está deshabilitada", req.getRequestURI())
        );
    }

    // Maneja AuthenticationException: clase base para cualquier error de autenticación de Spring Security
    // Actúa como captura genérica para errores de autenticación no cubiertos por los handlers anteriores
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                        "No autenticado: " + ex.getMessage(), req.getRequestURI())
        );
    }

    // Manejador genérico de último recurso: captura cualquier excepción no controlada por los handlers anteriores
    // Retorna HTTP 500 Internal Server Error para evitar que los stacktraces lleguen al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                // Si la excepción tiene mensaje, lo usa; si no, retorna un mensaje genérico
                ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                        ex.getMessage() != null ? ex.getMessage() : "Ocurrió un error inesperado en el servidor",
                        req.getRequestURI())
        );
    }
}

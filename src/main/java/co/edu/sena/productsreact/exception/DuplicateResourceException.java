package co.edu.sena.productsreact.exception;

// Excepción de negocio lanzada cuando se intenta crear un recurso que ya existe en la base de datos
// Por ejemplo: intentar registrar un usuario con un email que ya está en uso
// Al ser capturada por GlobalExceptionHandler, retorna automáticamente HTTP 409 Conflict
// Extiende RuntimeException: no requiere declararse en la firma del método (unchecked exception)
public class DuplicateResourceException extends RuntimeException {

    // Constructor que recibe el mensaje descriptivo del error
    // Ejemplo: "Ya existe un usuario registrado con el email: juan@example.com"
    public DuplicateResourceException(String message) {
        // Pasa el mensaje al constructor de RuntimeException para que esté disponible en ex.getMessage()
        super(message);
    }
}

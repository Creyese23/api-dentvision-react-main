package co.edu.sena.productsreact.exception;

// Excepción de negocio lanzada cuando se busca un recurso que no existe en la base de datos
// Por ejemplo: buscar un paciente por ID que no existe en la tabla "pacientes"
// Al ser capturada por GlobalExceptionHandler, retorna automáticamente HTTP 404 Not Found
// Extiende RuntimeException: no requiere declararse en la firma del método (unchecked exception)
public class ResourceNotFoundException extends RuntimeException {

    // Constructor que recibe el mensaje descriptivo del error
    // Ejemplo: "Paciente no encontrado con id: 42"
    public ResourceNotFoundException(String message) {
        // Pasa el mensaje al constructor de RuntimeException para que esté disponible en ex.getMessage()
        super(message);
    }
}

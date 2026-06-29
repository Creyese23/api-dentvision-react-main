package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "conversaciones"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;

// Fecha y hora sin zona horaria para los timestamps de inicio y fin de la conversación
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "conversaciones". Agrupa mensajes entre paciente y clínica
@Table(name = "conversaciones")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversacion {

    // Clave primaria de la tabla conversaciones, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Paciente: un paciente puede tener múltiples conversaciones
    // FetchType.LAZY: el paciente se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_paciente", es obligatoria
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    // Relación Muchos-a-Uno con Procedimiento: la conversación puede estar ligada a un procedimiento
    // FetchType.LAZY: el procedimiento se carga solo cuando se accede al campo
    // nullable = true (implícito): una conversación puede no estar asociada a un procedimiento
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_procedimiento". Puede ser null (conversación general)
    @JoinColumn(name = "id_procedimiento")
    private Procedimiento procedimiento;

    // Fecha y hora de inicio de la conversación. Obligatorio
    @Column(nullable = false)
    private LocalDateTime inicio;

    // Fecha y hora en que se cerró la conversación. null = la conversación sigue abierta
    @Column
    private LocalDateTime fin;
}

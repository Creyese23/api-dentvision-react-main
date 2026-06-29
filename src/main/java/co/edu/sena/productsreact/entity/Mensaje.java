package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "mensajes"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;

// Fecha y hora sin zona horaria para registrar el momento exacto del mensaje
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "mensajes". Cada fila es un mensaje dentro de una conversación
@Table(name = "mensajes")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mensaje {

    // Clave primaria de la tabla mensajes, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Conversacion: muchos mensajes pertenecen a una conversación
    // FetchType.LAZY: la conversación se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_conversacion", es obligatoria
    @JoinColumn(name = "id_conversacion", nullable = false)
    private Conversacion conversacion;

    // Relación Muchos-a-Uno con User: el usuario que envió el mensaje (paciente o empleado)
    // FetchType.LAZY: el usuario se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_remitente", es obligatoria
    @JoinColumn(name = "id_remitente", nullable = false)
    private User remitente;

    // Texto del mensaje. TEXT: permite mensajes de cualquier longitud. Obligatorio
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    // Fecha y hora exacta en que se envió el mensaje. Obligatorio
    @Column(nullable = false)
    private LocalDateTime hora;

    // Estado del mensaje. Valores posibles: "ENVIADO", "LEIDO", "ELIMINADO"
    @Column(nullable = false, length = 20)
    // @Builder.Default: cuando se crea con Builder, el estado inicial es "ENVIADO"
    @Builder.Default
    private String estado = "ENVIADO";
}

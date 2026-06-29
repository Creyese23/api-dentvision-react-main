package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "entregas"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;

// Fecha y hora sin zona horaria, usada para registrar cuándo se entregó la orden
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "entregas". Registra la entrega al paciente del resultado de una orden
@Table(name = "entregas")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Entrega {

    // Clave primaria de la tabla entregas, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Uno-a-Uno con Orden: cada entrega corresponde exactamente a una orden de trabajo
    // FetchType.LAZY: la orden se carga solo cuando se accede al campo
    @OneToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_orden", es obligatoria
    @JoinColumn(name = "id_orden", nullable = false)
    private Orden orden;

    // Estado de la entrega. Valores posibles: "PENDIENTE", "ENTREGADO", "CANCELADO"
    @Column(nullable = false, length = 30)
    // @Builder.Default: cuando se crea con Builder, el estado inicial es "PENDIENTE"
    @Builder.Default
    private String estado = "PENDIENTE";

    // Fecha y hora en que se realizó la entrega al paciente. null = aún no entregado
    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    // Observaciones sobre la entrega (condiciones, instrucciones al paciente, etc.). TEXT: sin límite
    @Column(columnDefinition = "TEXT")
    private String observaciones;
}

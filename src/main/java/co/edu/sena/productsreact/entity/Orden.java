package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "ordenes"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;
// Auditoría automática de fecha de creación
import org.springframework.data.annotation.CreatedDate;
// Listener de auditoría de Spring Data
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Fecha y hora sin zona horaria para el timestamp de creación
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "ordenes". Representa órdenes de trabajo generadas durante un procedimiento
@Table(name = "ordenes")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener de auditoría automática de Spring Data
@EntityListeners(AuditingEntityListener.class)
public class Orden {

    // Clave primaria de la tabla ordenes, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Procedimiento: una orden pertenece a un procedimiento específico
    // FetchType.LAZY: el procedimiento se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_procedimiento", es obligatoria
    @JoinColumn(name = "id_procedimiento", nullable = false)
    private Procedimiento procedimiento;

    // Descripción general de la orden de trabajo. TEXT: sin límite de longitud
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Imagen diagnóstica asociada (RX, foto intraoral). LONGTEXT: permite contenido Base64 extenso
    @Column(name = "imagen_diagnostica", columnDefinition = "LONGTEXT")
    private String imagenDiagnostica;

    @Column(name = "odontograma", columnDefinition = "LONGTEXT")
    private String odontograma;

    // Color/tono dental solicitado al laboratorio (ej: "A2", "B1")
    @Column(length = 30)
    private String color;

    // Estado de la orden. Valores posibles: "PENDIENTE", "EN_PROCESO", "COMPLETADA", "CANCELADA"
    @Column(nullable = false, length = 30)
    // @Builder.Default: cuando se crea con Builder, el estado inicial es "PENDIENTE"
    @Builder.Default
    private String estado = "PENDIENTE";

    // Fecha de creación de la orden. Spring Auditing la asigna automáticamente al insertar
    @CreatedDate
    // updatable = false: este valor no cambia después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}

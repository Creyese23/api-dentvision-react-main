package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "procedimientos"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;
// Auditoría automática de fecha de creación y última modificación
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
// Listener que activa la auditoría automática de Spring Data
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Tipo de dato para valores monetarios con precisión exacta
import java.math.BigDecimal;
// Fecha y hora sin zona horaria para timestamps
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "procedimientos". Un procedimiento es el trabajo clínico realizado en una cita
@Table(name = "procedimientos")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener de auditoría automática de Spring Data
@EntityListeners(AuditingEntityListener.class)
public class Procedimiento {

    // Clave primaria de la tabla procedimientos, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Cita: un procedimiento está asociado a una cita específica
    // FetchType.LAZY: la cita se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_cita", es obligatoria
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    // Relación Muchos-a-Uno con Empleado: el técnico dental que ejecuta el procedimiento
    // FetchType.LAZY: el empleado se carga solo cuando se accede al campo
    // nullable = true (implícito): un procedimiento puede no tener técnico asignado todavía
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_tecnico". Puede ser null (sin técnico asignado)
    @JoinColumn(name = "id_tecnico")
    private Empleado tecnico;

    // Nombre descriptivo del procedimiento (ej: "Extracción molar", "Obturación"). Obligatorio, máx 150 chars
    @Column(nullable = false, length = 150)
    private String nombre;

    // Descripción clínica detallada del procedimiento. TEXT: sin límite de longitud
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Costo del procedimiento. precision=12, scale=2: permite valores como 999999999.99
    @Column(precision = 12, scale = 2)
    private BigDecimal costo;

    // Estado del procedimiento. Valores posibles: "EN_PROCESO", "COMPLETADO", "CANCELADO"
    @Column(nullable = false, length = 30)
    // @Builder.Default: cuando se crea con Builder, el estado inicial es "EN_PROCESO"
    @Builder.Default
    private String estado = "EN_PROCESO";

    // Fecha y hora en que inició el procedimiento clínico
    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    // Fecha y hora en que finalizó el procedimiento clínico. null = aún en curso
    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // Observaciones o notas clínicas adicionales sobre el procedimiento. TEXT: sin límite
    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // Fecha de creación del registro. Spring Auditing la asigna automáticamente al insertar
    @CreatedDate
    // updatable = false: no cambia después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    // Fecha de última modificación. Spring Auditing la actualiza automáticamente en cada UPDATE
    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Fecha de borrado lógico del procedimiento. null = el procedimiento no ha sido eliminado
    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}

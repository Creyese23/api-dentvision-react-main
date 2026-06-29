package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "empleados"
import jakarta.persistence.*;
// Lombok: getters, setters, constructores y Builder automáticos
import lombok.*;
// Anotaciones de auditoría para registrar fechas de creación y modificación
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
// Listener que activa el mecanismo de auditoría automática de Spring Data
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Fecha y hora sin zona horaria, usada para timestamps
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "empleados"
@Table(name = "empleados")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Registra el listener de auditoría de Spring Data
@EntityListeners(AuditingEntityListener.class)
public class Empleado {

    // Clave primaria de la tabla empleados, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Uno-a-Uno con User: cada empleado tiene exactamente un usuario del sistema
    // FetchType.LAZY carga el usuario solo cuando se accede al campo
    @OneToOne(fetch = FetchType.LAZY)
    // La FK "id_usuario" es única: un usuario solo puede ser un empleado
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private User usuario;

    // Cargo o puesto del empleado en la clínica (ej: "Odontólogo", "Recepcionista", "Técnico")
    @Column(length = 60)
    private String cargo;

    // Género del empleado (ej: "M", "F", "OTRO")
    @Column(length = 20)
    private String genero;

    // Especialidad odontológica del empleado (ej: "Ortodoncia", "Endodoncia"). Puede ser null
    @Column(length = 120)
    private String especialidad;

    // Estado lógico del empleado: true = activo, false = desactivado
    @Column(nullable = false)
    // @Builder.Default garantiza que el valor inicial sea true al usar el Builder
    @Builder.Default
    private Boolean estado = true;

    // Fecha de creación del registro. La asigna automáticamente Spring Auditing al insertar
    @CreatedDate
    // updatable = false: no se puede modificar este campo después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    // Fecha de la última actualización. Spring Auditing la rellena automáticamente en cada UPDATE
    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Fecha de borrado lógico del empleado. null = el empleado está activo, no eliminado
    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}

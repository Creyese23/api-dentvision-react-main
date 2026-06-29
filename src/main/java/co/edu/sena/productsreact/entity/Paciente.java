package co.edu.sena.productsreact.entity;

// Importaciones de JPA para mapear la entidad a la tabla "pacientes"
import jakarta.persistence.*;
// Lombok: genera automáticamente getters, setters, constructores y Builder
import lombok.*;
// Anotaciones de auditoría para registrar fechas automáticamente
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
// Listener que activa la auditoría automática de Spring Data JPA
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Fecha sin hora (solo año, mes, día), usado para fecha de nacimiento
import java.time.LocalDate;
// Fecha y hora sin zona horaria, usada para timestamps de auditoría
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "pacientes"
@Table(name = "pacientes")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener que rellena automáticamente las fechas de auditoría
@EntityListeners(AuditingEntityListener.class)
public class Paciente {

    // Clave primaria de la tabla pacientes, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Uno-a-Uno con User: cada paciente corresponde a exactamente un usuario
    // FetchType.LAZY: el usuario se carga solo cuando se accede a este campo (optimiza consultas)
    @OneToOne(fetch = FetchType.LAZY)
    // La FK en la tabla pacientes es "id_usuario". Es obligatoria y única (un usuario = un paciente)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private User usuario;

    // Fecha de nacimiento del paciente (solo fecha, sin hora)
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    // Estado lógico del paciente: true = activo, false = inactivo (borrado lógico)
    @Column(nullable = false)
    // @Builder.Default establece el valor por defecto cuando se usa el patrón Builder
    @Builder.Default
    private Boolean estado = true;

    // Fecha en que se creó el registro. Spring Auditing la asigna automáticamente al insertar
    @CreatedDate
    // updatable = false: este campo nunca cambia después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    // Fecha de la última modificación del registro. Spring Auditing la actualiza automáticamente
    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Fecha en que se realizó el borrado lógico del paciente. null = el paciente no está eliminado
    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}

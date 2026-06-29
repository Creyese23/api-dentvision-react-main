package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "citas"
import jakarta.persistence.*;
// Lombok para generar automáticamente getters, setters, constructores y Builder
import lombok.*;
// Anotaciones de auditoría de Spring Data para fecha de creación y modificación
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
// Listener que activa la auditoría automática al guardar/actualizar
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Fecha y hora sin zona horaria para timestamps y la fecha de la cita
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "citas"
@Table(name = "citas")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa la auditoría automática de fechas con Spring Data
@EntityListeners(AuditingEntityListener.class)
public class Cita {

    // Clave primaria de la tabla citas, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Paciente: muchas citas pueden pertenecer a un mismo paciente
    // FetchType.LAZY: el paciente se carga solo cuando se accede al campo (evita consultas innecesarias)
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla citas es "id_paciente", campo obligatorio
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    // Relación Muchos-a-Uno con Empleado: un odontólogo puede tener muchas citas
    // FetchType.LAZY: el odontólogo se carga solo cuando se necesita
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla citas es "id_odontologo", campo obligatorio
    @JoinColumn(name = "id_odontologo", nullable = false)
    private Empleado odontologo;

    // Fecha y hora exacta en que está programada la cita (ej: 2026-07-10T10:30:00)
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // Motivo de la consulta, ingresado por el paciente o recepcionista. TEXT permite textos largos
    @Column(columnDefinition = "TEXT")
    private String motivo;

    // Estado actual de la cita. Valores posibles: "PROGRAMADA", "COMPLETADA", "CANCELADA", "EN_CURSO"
    @Column(nullable = false, length = 30)
    // @Builder.Default: cuando se crea una cita nueva con Builder, el estado inicial es "PROGRAMADA"
    @Builder.Default
    private String estado = "PROGRAMADA";

    // Fecha de creación del registro. Spring Auditing la asigna automáticamente al insertar
    @CreatedDate
    // updatable = false: este campo no se modifica después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    // Fecha de última modificación. Spring Auditing la actualiza en cada UPDATE
    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Fecha de borrado lógico. null = la cita no ha sido eliminada
    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}

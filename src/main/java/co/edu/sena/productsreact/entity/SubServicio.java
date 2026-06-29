package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "sub_servicios"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;
// Auditoría automática de fecha de creación
import org.springframework.data.annotation.CreatedDate;
// Listener de auditoría de Spring Data
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Tipo de dato para valores monetarios con precisión exacta
import java.math.BigDecimal;
// Fecha y hora sin zona horaria para el campo de auditoría
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "sub_servicios". Representa variantes o especialidades dentro de un servicio
@Table(name = "sub_servicios")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener de auditoría de Spring Data
@EntityListeners(AuditingEntityListener.class)
public class SubServicio {

    // Clave primaria de la tabla sub_servicios, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Servicio: un subservicio pertenece a un servicio padre
    // FetchType.LAZY: el servicio padre se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_servicio", es obligatoria
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    // Nombre del subservicio (ej: "Brackets metálicos", "Blanqueamiento LED"). Obligatorio, máx 120 chars
    @Column(nullable = false, length = 120)
    private String nombre;

    // Descripción detallada del subservicio. TEXT: sin límite de longitud
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Precio específico del subservicio. Puede ser null si se hereda del servicio padre
    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    // Duración estimada del subservicio en minutos (ej: 30, 60, 90). Puede ser null
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    // Estado lógico del subservicio: true = activo y disponible, false = desactivado
    @Column(nullable = false)
    // @Builder.Default: valor por defecto true al construir con Builder
    @Builder.Default
    private Boolean estado = true;

    // Fecha de creación del registro. Spring Auditing la asigna automáticamente al insertar
    @CreatedDate
    // updatable = false: no cambia después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}

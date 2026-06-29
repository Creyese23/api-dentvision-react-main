package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "servicios"
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
// Fecha y hora sin zona horaria para los campos de auditoría
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "servicios" (catálogo de servicios odontológicos de la clínica)
@Table(name = "servicios")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener de auditoría automática de fechas
@EntityListeners(AuditingEntityListener.class)
public class Servicio {

    // Clave primaria de la tabla servicios, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre del servicio odontológico (ej: "Ortodoncia", "Limpieza dental"). Obligatorio, máx 100 chars
    @Column(nullable = false, length = 100)
    private String nombre;

    // Descripción detallada del servicio. TEXT: sin límite de longitud
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // URL o contenido Base64 de la imagen representativa del servicio
    // LONGTEXT: permite imágenes codificadas en Base64 que pueden ser muy largas
    @Column(name = "imagen_url", columnDefinition = "LONGTEXT")
    private String imagenUrl;

    // Precio base del servicio en la moneda local. precision=12, scale=2: hasta 9999999999.99
    @Column(nullable = false, precision = 12, scale = 2)
    // @Builder.Default: cuando se crea con Builder sin especificar precio, el default es 0
    @Builder.Default
    private BigDecimal precio = BigDecimal.ZERO;

    // Estado lógico del servicio: true = disponible, false = desactivado (no se muestra al público)
    @Column(nullable = false)
    @Builder.Default
    private Boolean estado = true;

    // Fecha de creación del registro. Spring Auditing la asigna automáticamente
    @CreatedDate
    // updatable = false: este valor no cambia después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    // Fecha de última modificación. Spring Auditing la actualiza automáticamente en cada UPDATE
    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}

package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "insumos"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;
// Auditoría automática de fecha de creación y modificación
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
// Listener que activa la auditoría automática de Spring Data
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Tipo de dato para valores monetarios con precisión exacta
import java.math.BigDecimal;
// Fecha y hora sin zona horaria para timestamps de auditoría
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "insumos". Representa los materiales e insumos dentales del inventario
@Table(name = "insumos")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener de auditoría automática de Spring Data
@EntityListeners(AuditingEntityListener.class)
public class Insumo {

    // Clave primaria de la tabla insumos, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre del insumo (ej: "Amalgama dental", "Guantes de nitrilo"). Obligatorio, máx 100 chars
    @Column(nullable = false, length = 100)
    private String nombre;

    // Descripción detallada del insumo (especificaciones, uso, etc.). TEXT: sin límite de longitud
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Unidad de medida del insumo (ej: "unidades", "cajas", "mililitros", "gramos")
    @Column(name = "unidad_medida", length = 30)
    private String unidadMedida;

    // Cantidad actual disponible en el inventario. Obligatorio, default 0
    @Column(name = "stock_actual", nullable = false)
    // @Builder.Default: valor inicial 0 al construir con el patrón Builder
    @Builder.Default
    private Integer stockActual = 0;

    // Cantidad mínima permitida en el inventario. Si stockActual < stockMinimo, se genera alerta
    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 0;

    // Precio de compra por unidad del insumo. precision=12, scale=2 para manejo monetario exacto
    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    // Estado lógico del insumo: true = activo, false = dado de baja del inventario
    @Column(nullable = false)
    @Builder.Default
    private Boolean estado = true;

    // Fecha de creación del registro. Spring Auditing la asigna automáticamente al insertar
    @CreatedDate
    // updatable = false: no cambia después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    // Fecha de última modificación. Spring Auditing la actualiza en cada UPDATE
    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Fecha de borrado lógico del insumo. null = el insumo sigue activo
    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}

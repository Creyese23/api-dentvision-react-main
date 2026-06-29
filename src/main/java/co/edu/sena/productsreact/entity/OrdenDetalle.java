package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "orden_detalle"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;

// Tipo de dato para valores monetarios con precisión exacta
import java.math.BigDecimal;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "orden_detalle". Representa los ítems (líneas) de una orden de trabajo
@Table(name = "orden_detalle")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenDetalle {

    // Clave primaria de la tabla orden_detalle, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Orden: cada línea de detalle pertenece a una sola orden
    // FetchType.LAZY: la orden se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_orden", es obligatoria
    @JoinColumn(name = "id_orden", nullable = false)
    private Orden orden;

    // Relación Muchos-a-Uno con Servicio: cada línea referencia un servicio del catálogo
    // FetchType.LAZY: el servicio se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_servicio", es obligatoria
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    // Cantidad de unidades del servicio incluidas en esta línea del detalle. Obligatorio
    @Column(nullable = false)
    private Integer cantidad;

    // Precio por unidad del servicio en el momento de crear la orden. precision=12, scale=2
    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    // Observaciones o notas adicionales específicas de esta línea del detalle. TEXT: sin límite
    @Column(columnDefinition = "TEXT")
    private String observaciones;
}

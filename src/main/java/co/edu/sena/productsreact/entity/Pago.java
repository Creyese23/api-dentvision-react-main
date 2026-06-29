package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "pagos"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;
// Auditoría automática de fecha de creación y modificación
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
// Listener de auditoría de Spring Data
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Tipo de dato para valores monetarios con precisión exacta (evita errores de punto flotante)
import java.math.BigDecimal;
// Fecha y hora sin zona horaria para timestamps
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "pagos". Cada fila representa un pago realizado sobre una factura
@Table(name = "pagos")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener de auditoría automática de Spring Data
@EntityListeners(AuditingEntityListener.class)
public class Pago {

    // Clave primaria de la tabla pagos, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Factura: una factura puede tener múltiples pagos (abonos)
    // FetchType.LAZY: la factura se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_factura", es obligatoria
    @JoinColumn(name = "id_factura", nullable = false)
    private Factura factura;

    // Monto del pago. precision=14, scale=2: permite hasta 999999999999.99 (montos grandes)
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    // Método de pago usado: "EFECTIVO", "TARJETA", "TRANSFERENCIA", "PSE", "PAYPAL", etc.
    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;

    // Estado actual del pago. Valores posibles: "COMPLETADO", "PENDIENTE", "RECHAZADO", "REEMBOLSADO"
    @Column(nullable = false, length = 30)
    // @Builder.Default: cuando se crea con Builder, el estado inicial es "COMPLETADO"
    @Builder.Default
    private String estado = "COMPLETADO";

    // Fecha y hora exacta en que se realizó el pago (puede diferir de fecha_creacion)
    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    // Fecha de inserción del registro en la BD. Spring Auditing la asigna automáticamente
    @CreatedDate
    // updatable = false: no cambia después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    // Fecha de última modificación del registro. Spring Auditing la actualiza automáticamente
    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Fecha de borrado lógico del pago. null = el pago no ha sido anulado
    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;
}

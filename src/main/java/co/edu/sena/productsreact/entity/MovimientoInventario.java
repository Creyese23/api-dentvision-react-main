package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "movimientos_inventario"
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
// Tabla en BD: "movimientos_inventario". Registra cada entrada o salida de insumos del inventario
@Table(name = "movimientos_inventario")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener de auditoría automática de Spring Data
@EntityListeners(AuditingEntityListener.class)
public class MovimientoInventario {

    // Clave primaria de la tabla, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Insumo: un movimiento pertenece a un insumo específico
    // FetchType.LAZY: el insumo se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_insumo", es obligatoria
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;

    // Relación Muchos-a-Uno con Empleado: el empleado que registró el movimiento
    // FetchType.LAZY: el empleado se carga solo cuando se accede al campo
    // nullable = true (implícito): el movimiento puede no tener empleado asociado (ej: ajuste automático)
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_empleado". Puede ser null
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    // Cantidad de unidades que entraron o salieron del inventario. Obligatorio
    @Column(nullable = false)
    private Integer cantidad;

    // Tipo de movimiento: "ENTRADA" (compra/reposición) o "SALIDA" (uso en procedimiento/consumo)
    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private String tipoMovimiento;

    // Fecha de registro del movimiento. Spring Auditing la asigna automáticamente al insertar
    @CreatedDate
    // updatable = false: este campo no se modifica después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}

package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla de relación "citas_servicios"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;

// BigDecimal: tipo numérico de alta precisión para valores monetarios (evita errores de punto flotante)
import java.math.BigDecimal;

// Marca la clase como entidad JPA
@Entity
// Tabla en BD: "citas_servicios". Es una tabla intermedia que relaciona citas con servicios
@Table(name = "citas_servicios")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CitaServicio {

    // Clave primaria de la tabla, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con Cita: un registro de cita-servicio pertenece a una sola cita
    // FetchType.LAZY: la cita se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_cita", es obligatoria
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    // Relación Muchos-a-Uno con Servicio: en una cita se pueden asociar múltiples servicios
    // FetchType.LAZY: el servicio se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_servicio", es obligatoria
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    // Precio pactado para este servicio en esta cita específica
    // precision=12: hasta 12 dígitos totales; scale=2: 2 decimales (ej: 150000.00)
    @Column(name = "precio_acordado", precision = 12, scale = 2)
    private BigDecimal precioAcordado;

    // Observaciones o notas específicas sobre este servicio en la cita. TEXT permite textos largos
    @Column(columnDefinition = "TEXT")
    private String observaciones;
}

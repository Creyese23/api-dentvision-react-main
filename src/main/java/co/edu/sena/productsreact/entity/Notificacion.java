package co.edu.sena.productsreact.entity;

// Importaciones de Jakarta Persistence API (JPA) para mapear la clase a la tabla "notificaciones"
import jakarta.persistence.*;
// Importaciones de Lombok para evitar código boilerplate (getters, setters, constructores, builder)
import lombok.*;
// Evita bucle infinito de serialización Jackson: Notificacion→User→PerfilUsuario→User→...
import com.fasterxml.jackson.annotation.JsonIgnore;
// Anotación de auditoría de Spring Data para registrar la fecha de creación automáticamente
import org.springframework.data.annotation.CreatedDate;
// Listener que activa la auditoría automática de fechas al guardar la entidad
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Clase de fecha y hora sin zona horaria, usada para el campo creadaEn
import java.time.LocalDateTime;

// Indica que esta clase es una entidad JPA que se mapea a una tabla de la base de datos
@Entity
// Mapea la entidad a la tabla "notificaciones" en la base de datos
@Table(name = "notificaciones")
// Lombok: genera getters para todos los campos de la clase
@Getter
// Lombok: genera setters para todos los campos de la clase
@Setter
// Lombok: genera un constructor sin argumentos (requerido por JPA para la instanciación)
@NoArgsConstructor
// Lombok: genera un constructor con todos los argumentos de la clase
@AllArgsConstructor
// Lombok: habilita el patrón Builder para crear objetos de forma fluida (Notificacion.builder()...)
@Builder
// Registra el listener que rellena automáticamente el campo @CreatedDate al persistir la entidad
@EntityListeners(AuditingEntityListener.class)
public class Notificacion {

    // Clave primaria de la tabla notificaciones; la base de datos genera el valor automáticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con la entidad User: muchas notificaciones pueden pertenecer a un usuario
    // FetchType.LAZY carga el usuario solo cuando se accede al campo (evita consultas innecesarias)
    @ManyToOne(fetch = FetchType.LAZY)
    // La columna de unión en la tabla notificaciones es "id_usuario", no puede ser nula
    @JoinColumn(name = "id_usuario", nullable = false)
    // Excluye el objeto User completo del JSON: evita bucle infinito User→PerfilUsuario→User→...
    @JsonIgnore
    private User usuario;

    // Tipo de notificación; identifica la categoría del evento (ej: "LOGIN", "CITA", "PAGO")
    // Máximo 50 caracteres; obligatorio para clasificar la notificación correctamente
    @Column(nullable = false, length = 50)
    private String tipo;

    // Mensaje descriptivo de la notificación que se muestra al usuario en la interfaz
    // Máximo 500 caracteres; obligatorio para que el usuario entienda qué ocurrió
    @Column(nullable = false, length = 500)
    private String mensaje;

    // Indica si el usuario ya leyó la notificación: false = no leída (nueva), true = leída
    // @Builder.Default asegura que el valor inicial al usar el Builder sea false
    @Column(nullable = false)
    @Builder.Default
    private boolean leida = false;

    // Fecha y hora exactas en que se creó la notificación; Spring Auditing la rellena automáticamente
    // @CreatedDate: se asigna una sola vez al insertar el registro; nunca se modifica después
    @CreatedDate
    // updatable = false: impide que Hibernate actualice este campo en operaciones UPDATE
    @Column(name = "creada_en", updatable = false)
    private LocalDateTime creadaEn;
}

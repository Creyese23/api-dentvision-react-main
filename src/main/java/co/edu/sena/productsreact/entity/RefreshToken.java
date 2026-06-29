package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "refresh_tokens"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;
// Auditoría automática de fecha de creación
import org.springframework.data.annotation.CreatedDate;
// Listener de auditoría de Spring Data
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Fecha y hora sin zona horaria para la expiración y la fecha de creación
import java.time.LocalDateTime;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "refresh_tokens". Almacena tokens de renovación de sesión para cada usuario
@Table(name = "refresh_tokens")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// Activa el listener de auditoría automática de Spring Data
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    // Clave primaria de la tabla, generada automáticamente por la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación Muchos-a-Uno con User: un usuario puede tener múltiples refresh tokens (varios dispositivos)
    // FetchType.LAZY: el usuario se carga solo cuando se accede al campo
    @ManyToOne(fetch = FetchType.LAZY)
    // La FK en la tabla es "id_usuario", es obligatoria
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    // El valor del refresh token (cadena aleatoria larga y única, ej: UUID). unique = no se puede repetir
    // length=512: suficiente para tokens UUID o tokens cifrados más largos
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    // Fecha y hora en que expira este refresh token. Después de esta fecha, ya no es válido
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Indica si el token fue revocado manualmente (ej: al cerrar sesión o cambiar contraseña)
    // false = token activo y válido; true = token invalidado
    @Column(nullable = false)
    // @Builder.Default: cuando se crea con Builder, el token no está revocado (false)
    @Builder.Default
    private Boolean revoked = false;

    // Fecha de creación del registro del token. Spring Auditing la asigna automáticamente al insertar
    @CreatedDate
    // updatable = false: este campo no cambia después de la inserción
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}

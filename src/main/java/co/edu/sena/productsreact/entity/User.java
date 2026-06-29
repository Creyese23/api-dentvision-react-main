package co.edu.sena.productsreact.entity;

// Importaciones de Jakarta Persistence API (JPA) para mapear la clase a la base de datos
import jakarta.persistence.*;
// Importaciones de Lombok para generar automáticamente getters, setters, constructores y el patrón Builder
import lombok.*;
// Anotaciones de auditoría de Spring Data para registrar fechas de creación y modificación
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
// Listener que activa la auditoría automática de fechas al guardar/actualizar
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Clase de fecha y hora sin zona horaria, usada para timestamps
import java.time.LocalDateTime;

// Indica que esta clase es una entidad JPA (se mapea a una tabla en la BD)
@Entity
// Mapea esta entidad a la tabla "usuarios" y define una restricción única sobre el campo "email"
@Table(name = "usuarios", uniqueConstraints = {
        // Nombre de la restricción en BD: uk_usuarios_email. Evita emails duplicados
        @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email")
})
// Lombok: genera getters para todos los campos
@Getter
// Lombok: genera setters para todos los campos
@Setter
// Lombok: genera un constructor sin argumentos (requerido por JPA)
@NoArgsConstructor
// Lombok: genera un constructor con todos los argumentos
@AllArgsConstructor
// Lombok: habilita el patrón Builder para construir objetos de forma fluida
@Builder
// Registra el listener de auditoría que rellena automáticamente createdAt y updatedAt
@EntityListeners(AuditingEntityListener.class)
public class User {

    // Clave primaria de la tabla usuarios
    @Id
    // La BD genera el valor automáticamente con autoincremento (IDENTITY)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Columna email: obligatoria (NOT NULL) y con máximo 120 caracteres
    @Column(nullable = false, length = 120)
    private String email;

    // Columna password: almacena el hash BCrypt de la contraseña, obligatoria y máximo 100 chars
    @Column(nullable = false, length = 100)
    private String password;

    // Indica el proveedor de autenticación: "LOCAL", "GOOGLE" o "GITHUB". Null = registro local
    @Column(length = 20)
    private String provider; // LOCAL, GOOGLE, GITHUB (null = LOCAL)

    // Estado lógico del usuario: true = activo, false = desactivado. Nunca se elimina en BD
    @Column(nullable = false)
    // @Builder.Default asegura que al usar el Builder, el valor inicial sea true
    @Builder.Default
    private Boolean estado = true;

    // Relación Muchos-a-Uno con la entidad Rol: un usuario tiene un rol
    // FetchType.EAGER carga el rol inmediatamente junto con el usuario (necesario para Spring Security)
    @ManyToOne(fetch = FetchType.EAGER)
    // La columna de unión en la tabla usuarios es "id_rol", no puede ser nula
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    // Relación Uno-a-Uno bidireccional con PerfilUsuario
    // mappedBy indica que PerfilUsuario es el dueño de la relación (tiene la FK)
    // FetchType.LAZY carga el perfil solo cuando se accede al campo (ahorra consultas)
    // CascadeType.ALL: cualquier operación sobre User se propaga al PerfilUsuario
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private PerfilUsuario perfil;

    // Indica si el usuario ya verificó su correo electrónico. false = pendiente de verificación
    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    // Token único enviado por correo para que el usuario verifique su cuenta
    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    // Token temporal para restablecer la contraseña (se envía al email del usuario)
    @Column(name = "reset_token", length = 100)
    private String resetToken;

    // Fecha y hora en que expira el token de restablecimiento de contraseña
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // Fecha de creación del registro. Spring Auditing la llena automáticamente al insertar
    @CreatedDate
    // updatable = false: una vez insertado, este valor no se modifica nunca
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Fecha de última modificación. Spring Auditing la actualiza automáticamente en cada UPDATE
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

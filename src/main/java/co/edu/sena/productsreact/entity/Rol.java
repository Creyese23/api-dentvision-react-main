package co.edu.sena.productsreact.entity;

// Importaciones de JPA para mapear la clase a la base de datos
import jakarta.persistence.*;
// Lombok: genera getters, setters, constructores y Builder automáticamente
import lombok.*;

// Marca la clase como entidad JPA (representa una fila en la tabla "roles")
@Entity
// Nombre de la tabla en la base de datos
@Table(name = "roles")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rol {

    // Clave primaria de la tabla roles
    @Id
    // El valor se genera automáticamente con autoincremento en la BD
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre del rol (ej: "ROLE_ADMIN", "ROLE_PACIENTE"). Obligatorio, único y máximo 50 chars
    // Spring Security requiere que los roles comiencen con "ROLE_"
    @Column(name = "nombre_rol", nullable = false, unique = true, length = 50)
    private String nombreRol;
}

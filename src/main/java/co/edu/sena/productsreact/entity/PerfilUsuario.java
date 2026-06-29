package co.edu.sena.productsreact.entity;

// Importaciones JPA para mapear la entidad a la tabla "perfiles_usuario"
import jakarta.persistence.*;
// Lombok para getters, setters, constructores y Builder
import lombok.*;

// Marca la clase como entidad JPA
@Entity
// La tabla en BD se llama "perfiles_usuario". Contiene los datos personales del usuario
@Table(name = "perfiles_usuario")
// Lombok: getters, setters, constructor vacío, constructor completo y Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerfilUsuario {

    // La clave primaria comparte el mismo valor que el id del usuario relacionado
    // Esto es un patrón de tabla derivada: la PK es también la FK hacia "usuarios"
    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    // Relación Uno-a-Uno con User. @MapsId indica que el id de PerfilUsuario proviene de User.id
    // FetchType.LAZY: el usuario se carga solo cuando se accede al campo
    @OneToOne(fetch = FetchType.LAZY)
    // @MapsId: el campo "id_usuario" de esta tabla actúa como PK y FK al mismo tiempo
    @MapsId
    @JoinColumn(name = "id_usuario")
    private User usuario;

    // Nombres del usuario (ej: "Juan Carlos"). Máx 100 chars. DEFAULT '' para evitar nulls en BD
    @Column(length = 100, columnDefinition = "VARCHAR(100) NULL DEFAULT ''")
    private String nombres;

    // Apellidos del usuario (ej: "Pérez García"). Máx 100 chars. DEFAULT '' para evitar nulls en BD
    @Column(length = 100, columnDefinition = "VARCHAR(100) NULL DEFAULT ''")
    private String apellidos;

    // Tipo de documento de identidad (ej: "CC", "TI", "CE", "PASAPORTE"). DEFAULT ''
    @Column(name = "tipo_identificacion", length = 30, columnDefinition = "VARCHAR(30) NULL DEFAULT ''")
    private String tipoIdentificacion;

    // Número de identificación del usuario (ej: "1012345678"). Máx 20 chars. DEFAULT ''
    @Column(length = 20, columnDefinition = "VARCHAR(20) NULL DEFAULT ''")
    private String identificacion;

    // Número de teléfono de contacto del usuario (ej: "3001234567"). Máx 20 chars. DEFAULT ''
    @Column(length = 20, columnDefinition = "VARCHAR(20) NULL DEFAULT ''")
    private String telefono;

    // Dirección de residencia del usuario (ej: "Calle 10 #5-23, Bogotá"). Máx 200 chars. DEFAULT ''
    @Column(length = 200, columnDefinition = "VARCHAR(200) NULL DEFAULT ''")
    private String direccion;

    // Foto de perfil del usuario almacenada como Base64 o URL externa
    // LONGTEXT: necesario porque las imágenes Base64 son cadenas muy largas
    @Column(name = "foto_perfil", columnDefinition = "LONGTEXT")
    private String fotoPerfil;

    // Género del usuario (ej: "MASCULINO", "FEMENINO", "OTRO"). Máx 30 chars. Puede ser null
    @Column(length = 30)
    private String genero;
}

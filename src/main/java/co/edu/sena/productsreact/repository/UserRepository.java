package co.edu.sena.productsreact.repository;

// Importa la entidad User sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.User;
// JpaRepository: interfaz de Spring Data JPA que provee operaciones CRUD básicas
// El primer parámetro es la entidad, el segundo es el tipo de la clave primaria
import org.springframework.data.jpa.repository.JpaRepository;
// Marca la interfaz como un componente de repositorio de Spring (se puede inyectar con @Autowired)
import org.springframework.stereotype.Repository;

// Lista para resultados con múltiples registros
import java.util.List;
// Optional: contenedor que puede contener un User o estar vacío (evita NullPointerExceptions)
import java.util.Optional;

// @Repository: declara que esta interfaz es un bean de repositorio gestionado por Spring
@Repository
// Extiende JpaRepository<User, Long>: hereda métodos como save(), findById(), findAll(), delete(), etc.
public interface UserRepository extends JpaRepository<User, Long> {

    // Consulta derivada: SELECT * FROM usuarios WHERE email = ?
    // Retorna Optional porque puede no existir un usuario con ese email
    // Usada en autenticación para buscar el usuario por su correo
    Optional<User> findByEmail(String email);

    // Consulta derivada: SELECT COUNT(*) > 0 FROM usuarios WHERE email = ?
    // Retorna true si ya existe un usuario con ese email (para validar duplicados en registro)
    boolean existsByEmail(String email);

    // Consulta derivada: SELECT COUNT(*) > 0 FROM usuarios u JOIN roles r ON u.id_rol = r.id WHERE r.nombre_rol = ?
    // Usada para verificar si ya existe al menos un usuario con un rol específico (ej: para el primer admin)
    boolean existsByRolNombreRol(String nombreRol);

    // Consulta derivada: SELECT * FROM usuarios u JOIN roles r ON u.id_rol = r.id WHERE r.nombre_rol = ? AND u.estado = true
    // Devuelve todos los usuarios activos con un rol específico (ej: listar todos los odontólogos activos)
    List<User> findByRolNombreRolAndEstadoTrue(String nombreRol);

    // Consulta derivada con IN: SELECT * FROM usuarios u JOIN roles r ON u.id_rol = r.id WHERE r.nombre_rol IN (?) AND u.estado = true
    // Devuelve usuarios activos que tengan cualquiera de los roles de la lista (ej: odontólogos Y técnicos)
    List<User> findByRolNombreRolInAndEstadoTrue(List<String> roles);

    // Consulta derivada: SELECT * FROM usuarios WHERE reset_token = ?
    // Usada para encontrar un usuario por su token de restablecimiento de contraseña
    Optional<User> findByResetToken(String resetToken);

    // Consulta derivada: SELECT * FROM usuarios WHERE verification_token = ?
    // Usada para verificar la cuenta de un usuario cuando hace clic en el enlace del email
    Optional<User> findByVerificationToken(String verificationToken);
}

package co.edu.sena.productsreact.repository;

// Importa la entidad Paciente sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.Paciente;
// Page: contenedor de resultados paginados (incluye datos de página actual, total, etc.)
import org.springframework.data.domain.Page;
// Pageable: parámetro que lleva la información de paginación y ordenamiento
import org.springframework.data.domain.Pageable;
// JpaRepository: provee operaciones CRUD básicas para la entidad
import org.springframework.data.jpa.repository.JpaRepository;
// @Query: permite escribir consultas JPQL personalizadas directamente en el repositorio
import org.springframework.data.jpa.repository.Query;
// @Repository: declara este componente como bean de repositorio de Spring
import org.springframework.stereotype.Repository;

// Lista para resultados con múltiples registros
import java.util.List;
// Optional: contenedor para resultados que pueden no existir
import java.util.Optional;

// @Repository: Spring detecta esta interfaz y crea una implementación automática en tiempo de ejecución
@Repository
// JpaRepository<Paciente, Long>: hereda CRUD completo para la entidad Paciente con PK de tipo Long
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    // Consulta derivada: SELECT * FROM pacientes WHERE estado = true AND fecha_eliminacion IS NULL
    // Devuelve todos los pacientes activos que no han sido eliminados lógicamente
    List<Paciente> findByEstadoTrueAndFechaEliminacionIsNull();

    // Consulta derivada: SELECT * FROM pacientes WHERE id_usuario = ?
    // Permite encontrar el registro de paciente asociado a un usuario del sistema por su ID
    Optional<Paciente> findByUsuarioId(Long idUsuario);

    // Consulta derivada: SELECT COUNT(*) > 0 FROM pacientes WHERE id_usuario = ?
    // Verifica si un usuario ya tiene un perfil de paciente registrado (evita duplicados)
    boolean existsByUsuarioId(Long idUsuario);

    // Consulta JPQL personalizada para buscar pacientes por nombre, apellido o número de identificación
    // JOIN p.usuario u: une la tabla pacientes con usuarios
    // JOIN u.perfil pu: une con la tabla perfiles_usuario para acceder a nombres, apellidos e identificacion
    // WHERE p.fechaEliminacion IS NULL: excluye pacientes eliminados lógicamente
    // LOWER(...) LIKE LOWER(CONCAT('%',:q,'%')): búsqueda insensible a mayúsculas/minúsculas
    // Retorna un Page<Paciente>: resultado paginado para listas grandes
    @Query("SELECT p FROM Paciente p " +
           "JOIN p.usuario u " +
           "JOIN u.perfil pu " +
           "WHERE p.fechaEliminacion IS NULL AND " +
           "(LOWER(pu.nombres) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(pu.apellidos) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " pu.identificacion LIKE CONCAT('%',:q,'%'))")
    // :q es el parámetro de búsqueda; Pageable controla página, tamaño y ordenamiento
    Page<Paciente> buscar(String q, Pageable pageable);
}

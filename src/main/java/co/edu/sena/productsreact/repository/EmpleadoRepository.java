package co.edu.sena.productsreact.repository;

// Importa la entidad Empleado sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.Empleado;
// JpaRepository: provee operaciones CRUD para la entidad
import org.springframework.data.jpa.repository.JpaRepository;
// @Repository: declara la interfaz como bean de repositorio de Spring
import org.springframework.stereotype.Repository;

// Lista para devolver múltiples resultados
import java.util.List;
// Optional: contenedor para resultados que pueden no existir
import java.util.Optional;

// @Repository: Spring crea la implementación automáticamente al iniciar la aplicación
@Repository
// JpaRepository<Empleado, Long>: hereda CRUD completo para la entidad Empleado con PK Long
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    // Consulta derivada: SELECT * FROM empleados WHERE estado = true AND fecha_eliminacion IS NULL
    // Retorna solo los empleados activos y no eliminados lógicamente (para listas generales)
    List<Empleado> findByEstadoTrueAndFechaEliminacionIsNull();

    // Consulta derivada: SELECT * FROM empleados WHERE cargo = ? AND estado = true AND fecha_eliminacion IS NULL
    // Retorna empleados activos filtrados por cargo específico (ej: "ODONTOLOGO", "TECNICO")
    // Útil para mostrar solo odontólogos al agendar citas
    List<Empleado> findByCargoAndEstadoTrueAndFechaEliminacionIsNull(String cargo);

    // Consulta derivada: SELECT * FROM empleados WHERE id_usuario = ?
    // Permite encontrar el registro de empleado vinculado a un usuario del sistema
    Optional<Empleado> findByUsuarioId(Long idUsuario);

    // Consulta derivada: SELECT COUNT(*) > 0 FROM empleados WHERE id_usuario = ?
    // Verifica si un usuario ya tiene perfil de empleado (evita duplicados al crear empleados)
    boolean existsByUsuarioId(Long idUsuario);
}

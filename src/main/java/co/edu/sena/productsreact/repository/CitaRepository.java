package co.edu.sena.productsreact.repository;

// Importa la entidad Cita sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.Cita;
// JpaRepository: provee operaciones CRUD para la entidad
import org.springframework.data.jpa.repository.JpaRepository;
// @Query: permite escribir consultas JPQL personalizadas
import org.springframework.data.jpa.repository.Query;
// @Repository: declara la interfaz como bean de repositorio de Spring
import org.springframework.stereotype.Repository;

// LocalDateTime: tipo usado en los parámetros de búsqueda por rango de fechas
import java.time.LocalDateTime;
// Lista para devolver múltiples resultados
import java.util.List;

// @Repository: Spring detecta e implementa esta interfaz automáticamente
@Repository
// JpaRepository<Cita, Long>: hereda CRUD completo para la entidad Cita con PK Long
public interface CitaRepository extends JpaRepository<Cita, Long> {

    // Consulta derivada: SELECT * FROM citas WHERE id_paciente = ? AND fecha_eliminacion IS NULL
    // Retorna todas las citas activas (no eliminadas) de un paciente específico
    List<Cita> findByPacienteIdAndFechaEliminacionIsNull(Long pacienteId);

    // Consulta derivada: SELECT * FROM citas WHERE id_odontologo = ? AND fecha_eliminacion IS NULL
    // Retorna todas las citas activas asignadas a un odontólogo específico
    List<Cita> findByOdontologoIdAndFechaEliminacionIsNull(Long odontologoId);

    // Consulta JPQL: SELECT * FROM citas WHERE fecha_eliminacion IS NULL AND fecha_hora BETWEEN :inicio AND :fin
    // Retorna todas las citas no eliminadas programadas dentro de un rango de fechas
    // Útil para mostrar el calendario de citas de un período específico
    @Query("SELECT c FROM Cita c WHERE c.fechaEliminacion IS NULL AND c.fechaHora BETWEEN :inicio AND :fin")
    List<Cita> findByRangoFecha(LocalDateTime inicio, LocalDateTime fin);

    // Consulta JPQL: filtra por odontólogo Y rango de fechas simultáneamente
    // Útil para ver la agenda de un odontólogo en una semana o mes específico
    // :odontologoId, :inicio, :fin son los parámetros que Spring inyecta desde el método
    @Query("SELECT c FROM Cita c WHERE c.fechaEliminacion IS NULL AND c.odontologo.id = :odontologoId AND c.fechaHora BETWEEN :inicio AND :fin")
    List<Cita> findByOdontologoAndRangoFecha(Long odontologoId, LocalDateTime inicio, LocalDateTime fin);

    // Consulta JPQL: busca citas "PROGRAMADA" cuya fecha ya pasó (vencidas sin completar)
    // c.fechaHora < :ahora significa que la hora de la cita ya ocurrió pero sigue en estado PROGRAMADA
    // Útil para un proceso automático que marca citas vencidas como "NO_ASISTIO" o similar
    @Query("SELECT c FROM Cita c WHERE c.fechaEliminacion IS NULL AND c.estado = 'PROGRAMADA' AND c.fechaHora < :ahora")
    List<Cita> findProgramadasVencidas(LocalDateTime ahora);
}

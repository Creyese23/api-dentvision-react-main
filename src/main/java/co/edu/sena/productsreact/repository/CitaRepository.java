package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByPacienteIdAndFechaEliminacionIsNull(Long pacienteId);

    List<Cita> findByOdontologoIdAndFechaEliminacionIsNull(Long odontologoId);

    @Query("SELECT c FROM Cita c WHERE c.fechaEliminacion IS NULL AND c.fechaHora BETWEEN :inicio AND :fin")
    List<Cita> findByRangoFecha(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT c FROM Cita c WHERE c.fechaEliminacion IS NULL AND c.odontologo.id = :odontologoId AND c.fechaHora BETWEEN :inicio AND :fin")
    List<Cita> findByOdontologoAndRangoFecha(Long odontologoId, LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT c FROM Cita c WHERE c.fechaEliminacion IS NULL AND c.estado = 'PROGRAMADA' AND c.fechaHora < :ahora")
    List<Cita> findProgramadasVencidas(LocalDateTime ahora);
}

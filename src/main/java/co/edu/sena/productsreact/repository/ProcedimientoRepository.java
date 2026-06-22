package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Procedimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcedimientoRepository extends JpaRepository<Procedimiento, Long> {

    @Query("SELECT p FROM Procedimiento p LEFT JOIN FETCH p.cita LEFT JOIN FETCH p.tecnico t LEFT JOIN FETCH t.usuario u LEFT JOIN FETCH u.perfil WHERE p.id = :id AND p.fechaEliminacion IS NULL")
    Optional<Procedimiento> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM Procedimiento p LEFT JOIN FETCH p.cita LEFT JOIN FETCH p.tecnico t LEFT JOIN FETCH t.usuario u LEFT JOIN FETCH u.perfil WHERE p.cita.id = :citaId AND p.fechaEliminacion IS NULL")
    List<Procedimiento> findByCitaIdWithDetails(@Param("citaId") Long citaId);

    @Query("SELECT p FROM Procedimiento p LEFT JOIN FETCH p.cita LEFT JOIN FETCH p.tecnico t LEFT JOIN FETCH t.usuario u LEFT JOIN FETCH u.perfil WHERE t.id = :tecnicoId AND p.fechaEliminacion IS NULL")
    List<Procedimiento> findByTecnicoIdWithDetails(@Param("tecnicoId") Long tecnicoId);

    @Query("SELECT p FROM Procedimiento p LEFT JOIN FETCH p.cita c LEFT JOIN FETCH c.paciente LEFT JOIN FETCH p.tecnico t LEFT JOIN FETCH t.usuario u LEFT JOIN FETCH u.perfil WHERE c.paciente.id = :pacienteId AND p.fechaEliminacion IS NULL")
    List<Procedimiento> findByCitaPacienteIdWithDetails(@Param("pacienteId") Long pacienteId);

    List<Procedimiento> findByCitaIdAndFechaEliminacionIsNull(Long citaId);
    List<Procedimiento> findByTecnicoIdAndFechaEliminacionIsNull(Long tecnicoId);
    List<Procedimiento> findByCitaPacienteIdAndFechaEliminacionIsNull(Long pacienteId);
}

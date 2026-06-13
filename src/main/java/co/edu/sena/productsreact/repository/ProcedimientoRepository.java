package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Procedimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedimientoRepository extends JpaRepository<Procedimiento, Long> {
    List<Procedimiento> findByCitaIdAndFechaEliminacionIsNull(Long citaId);
    List<Procedimiento> findByTecnicoIdAndFechaEliminacionIsNull(Long tecnicoId);
    List<Procedimiento> findByCitaPacienteIdAndFechaEliminacionIsNull(Long pacienteId);
}

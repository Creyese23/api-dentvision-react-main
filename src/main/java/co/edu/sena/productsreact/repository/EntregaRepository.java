package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    Optional<Entrega> findByOrdenId(Long ordenId);

    @Query("SELECT e FROM Entrega e WHERE e.orden.procedimiento.cita.paciente.id = :pacienteId")
    List<Entrega> findByPacienteId(@Param("pacienteId") Long pacienteId);
}

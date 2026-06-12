package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    List<Paciente> findByEstadoTrue();

    @Query("SELECT p FROM Paciente p WHERE p.fechaEliminacion IS NULL AND " +
           "(LOWER(p.nombres) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(p.apellidos) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " p.documento LIKE CONCAT('%',:q,'%'))")
    Page<Paciente> buscar(String q, Pageable pageable);

    boolean existsByDocumento(String documento);
}

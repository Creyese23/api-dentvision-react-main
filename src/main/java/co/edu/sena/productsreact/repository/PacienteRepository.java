package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    List<Paciente> findByEstadoTrueAndFechaEliminacionIsNull();

    Optional<Paciente> findByUsuarioId(Long idUsuario);

    boolean existsByUsuarioId(Long idUsuario);

    @Query("SELECT p FROM Paciente p " +
           "JOIN p.usuario u " +
           "JOIN u.perfil pu " +
           "WHERE p.fechaEliminacion IS NULL AND " +
           "(LOWER(pu.nombres) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(pu.apellidos) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " pu.identificacion LIKE CONCAT('%',:q,'%'))")
    Page<Paciente> buscar(String q, Pageable pageable);
}

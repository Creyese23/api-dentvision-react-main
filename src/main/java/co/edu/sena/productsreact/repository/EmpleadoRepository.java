package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    List<Empleado> findByEstadoTrue();
    List<Empleado> findByCargo(String cargo);
    Optional<Empleado> findByUsuarioId(Long idUsuario);
    boolean existsByUsuarioId(Long idUsuario);
}

package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    List<Empleado> findByEstadoTrue();
    List<Empleado> findByCargo(String cargo);
    boolean existsByDocumento(String documento);
}

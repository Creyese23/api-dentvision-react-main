package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {
    List<Orden> findByProcedimientoId(Long procedimientoId);
    List<Orden> findByEstado(String estado);
}

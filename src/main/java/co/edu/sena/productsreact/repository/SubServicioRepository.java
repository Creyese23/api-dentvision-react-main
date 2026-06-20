package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.SubServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubServicioRepository extends JpaRepository<SubServicio, Long> {
    List<SubServicio> findByServicioIdAndEstadoTrue(Long servicioId);
    List<SubServicio> findByServicioId(Long servicioId);
}

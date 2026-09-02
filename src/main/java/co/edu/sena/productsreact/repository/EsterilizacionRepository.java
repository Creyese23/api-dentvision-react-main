package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Esterilizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EsterilizacionRepository extends JpaRepository<Esterilizacion, Long> {
    Optional<Esterilizacion> findByCitaId(Long citaId);
}

package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findByConversacionIdOrderByHoraAsc(Long conversacionId);
}

package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    List<Servicio> findByEstadoTrue();
    boolean existsByNombre(String nombre);
    java.util.Optional<Servicio> findFirstByNombre(String nombre);
}

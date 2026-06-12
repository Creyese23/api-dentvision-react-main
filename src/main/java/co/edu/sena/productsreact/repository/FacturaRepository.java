package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    List<Factura> findByPacienteIdAndFechaEliminacionIsNull(Long pacienteId);
    List<Factura> findByEstadoAndFechaEliminacionIsNull(String estado);
}

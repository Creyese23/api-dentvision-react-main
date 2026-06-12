package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByFacturaIdAndFechaEliminacionIsNull(Long facturaId);

    @Query("SELECT COALESCE(SUM(p.valor),0) FROM Pago p WHERE p.factura.id = :facturaId AND p.fechaEliminacion IS NULL")
    BigDecimal sumValorByFacturaId(Long facturaId);
}

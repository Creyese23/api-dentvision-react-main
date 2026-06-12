package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    List<Insumo> findByEstadoTrueAndFechaEliminacionIsNull();

    @Query("SELECT i FROM Insumo i WHERE i.stockActual <= i.stockMinimo AND i.fechaEliminacion IS NULL")
    List<Insumo> findBajoStock();
}

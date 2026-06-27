package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.ProcedimientoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedimientoInsumoRepository extends JpaRepository<ProcedimientoInsumo, Long> {

    @Query("SELECT pi FROM ProcedimientoInsumo pi JOIN FETCH pi.insumo WHERE pi.procedimiento.id = :procedimientoId")
    List<ProcedimientoInsumo> findByProcedimientoId(Long procedimientoId);
}

package co.edu.sena.productsreact.repository;

// Importa la entidad Pago sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.Pago;
// JpaRepository: provee operaciones CRUD para la entidad
import org.springframework.data.jpa.repository.JpaRepository;
// @Query: permite escribir consultas JPQL personalizadas
import org.springframework.data.jpa.repository.Query;
// @Repository: declara la interfaz como bean de repositorio de Spring
import org.springframework.stereotype.Repository;

// BigDecimal: tipo de dato para valores monetarios con precisión exacta
import java.math.BigDecimal;
// Lista para devolver múltiples resultados
import java.util.List;

// @Repository: Spring implementa esta interfaz automáticamente al iniciar la aplicación
@Repository
// JpaRepository<Pago, Long>: hereda CRUD completo para la entidad Pago con PK Long
public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Consulta derivada: SELECT * FROM pagos WHERE id_factura = ? AND fecha_eliminacion IS NULL
    // Retorna todos los pagos no eliminados asociados a una factura (permite ver los abonos)
    List<Pago> findByFacturaIdAndFechaEliminacionIsNull(Long facturaId);

    // Consulta JPQL con función de agregación COALESCE(SUM(...), 0):
    // Suma el valor total de todos los pagos no eliminados de una factura específica
    // COALESCE(..., 0): si no hay pagos o la suma es null, devuelve 0 en lugar de null
    // Útil para calcular el saldo pagado de una factura y determinar si está al día
    @Query("SELECT COALESCE(SUM(p.valor),0) FROM Pago p WHERE p.factura.id = :facturaId AND p.fechaEliminacion IS NULL")
    BigDecimal sumValorByFacturaId(Long facturaId);
}

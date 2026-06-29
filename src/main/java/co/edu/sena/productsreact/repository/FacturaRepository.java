package co.edu.sena.productsreact.repository;

// Importa la entidad Factura sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.Factura;
// JpaRepository: provee operaciones CRUD para la entidad
import org.springframework.data.jpa.repository.JpaRepository;
// @Repository: declara la interfaz como bean de repositorio de Spring
import org.springframework.stereotype.Repository;

// Lista para devolver múltiples resultados
import java.util.List;

// @Repository: Spring implementa esta interfaz automáticamente al iniciar la aplicación
@Repository
// JpaRepository<Factura, Long>: hereda CRUD completo para la entidad Factura con PK Long
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    // Consulta derivada: SELECT * FROM facturas WHERE id_paciente = ? AND fecha_eliminacion IS NULL
    // Retorna todas las facturas activas (no eliminadas lógicamente) de un paciente específico
    // Útil para mostrar el historial de facturación de un paciente
    List<Factura> findByPacienteIdAndFechaEliminacionIsNull(Long pacienteId);

    // Consulta derivada: SELECT * FROM facturas WHERE estado = ? AND fecha_eliminacion IS NULL
    // Filtra facturas activas por estado (ej: "PENDIENTE", "PAGADA", "ANULADA")
    // Útil para dashboards de contabilidad o reportes de cartera
    List<Factura> findByEstadoAndFechaEliminacionIsNull(String estado);
}

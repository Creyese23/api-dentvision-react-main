package co.edu.sena.productsreact.repository;

// Importa la entidad Orden sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.Orden;
// JpaRepository: provee operaciones CRUD para la entidad
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
// @Repository: declara la interfaz como bean de repositorio de Spring
import org.springframework.stereotype.Repository;

// Lista para devolver múltiples resultados
import java.util.List;
import java.util.Optional;

// @Repository: Spring implementa esta interfaz automáticamente al iniciar la aplicación
@Repository
// JpaRepository<Orden, Long>: hereda CRUD completo para la entidad Orden con PK Long
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    // Consulta derivada: SELECT * FROM ordenes WHERE id_procedimiento = ?
    // Retorna todas las órdenes de trabajo asociadas a un procedimiento específico
    // Una orden pertenece a un procedimiento, y un procedimiento puede tener varias órdenes
    List<Orden> findByProcedimientoId(Long procedimientoId);

    // Consulta derivada: SELECT * FROM ordenes WHERE estado = ?
    // Filtra órdenes por su estado (ej: "PENDIENTE", "EN_PROCESO", "COMPLETADA", "CANCELADA")
    // Útil para el panel de gestión de órdenes de trabajo
    List<Orden> findByEstado(String estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orden o where o.id = :id")
    Optional<Orden> findByIdForUpdate(@Param("id") Long id);
}

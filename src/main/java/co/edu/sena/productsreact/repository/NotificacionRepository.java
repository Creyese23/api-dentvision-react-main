package co.edu.sena.productsreact.repository;

// Importa la entidad Notificacion sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.Notificacion;
// JpaRepository: interfaz de Spring Data JPA que provee operaciones CRUD básicas (save, findById, etc.)
import org.springframework.data.jpa.repository.JpaRepository;
// Permite escribir consultas JPQL personalizadas directamente en los métodos del repositorio
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
// Permite declarar parámetros nombrados en las consultas @Query (ej: :uid)
import org.springframework.data.repository.query.Param;
// Marca la interfaz como un componente de repositorio de Spring (inyectable con @Autowired)
import org.springframework.stereotype.Repository;

// Lista para devolver múltiples notificaciones ordenadas
import java.util.List;

// @Repository: Spring crea la implementación en tiempo de ejecución y la registra como bean
@Repository
// Extiende JpaRepository<Notificacion, Long>: hereda save(), findById(), findAll(), delete(), etc.
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Retorna todas las notificaciones de un usuario, ordenadas de la más reciente a la más antigua.
     * Spring Data JPA genera automáticamente la consulta SQL a partir del nombre del método.
     *
     * @param usuarioId ID del usuario propietario de las notificaciones
     * @return Lista de notificaciones ordenadas por creadaEn descendente
     */
    List<Notificacion> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    /**
     * Cuenta cuántas notificaciones no leídas tiene un usuario.
     * Usado para mostrar el badge de notificaciones pendientes en el frontend.
     *
     * @param usuarioId ID del usuario
     * @return Número de notificaciones con leida = false
     */
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    /**
     * Marca todas las notificaciones de un usuario como leídas en una sola operación SQL.
     * @Modifying indica que esta consulta modifica datos (no es un SELECT).
     * El parámetro clearAutomatically = true limpia el cache de Hibernate después del UPDATE,
     * evitando inconsistencias si se consultan notificaciones inmediatamente después.
     *
     * @param uid ID del usuario cuyas notificaciones se marcarán como leídas
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :uid")
    void marcarTodasLeidasPorUsuarioId(@Param("uid") Long uid);
}

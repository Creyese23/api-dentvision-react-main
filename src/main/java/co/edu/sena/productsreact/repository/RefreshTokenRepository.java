package co.edu.sena.productsreact.repository;

// Importa la entidad RefreshToken sobre la que opera este repositorio
import co.edu.sena.productsreact.entity.RefreshToken;
// JpaRepository: provee operaciones CRUD para la entidad
import org.springframework.data.jpa.repository.JpaRepository;
// @Modifying: indica que la consulta modifica datos (UPDATE o DELETE), no solo los lee
import org.springframework.data.jpa.repository.Modifying;
// @Query: permite escribir consultas JPQL personalizadas
import org.springframework.data.jpa.repository.Query;
// @Repository: declara la interfaz como bean de repositorio de Spring
import org.springframework.stereotype.Repository;

// Optional: contenedor para resultados que pueden no existir
import java.util.Optional;

// @Repository: Spring implementa esta interfaz automáticamente al arrancar la aplicación
@Repository
// JpaRepository<RefreshToken, Long>: hereda CRUD completo para RefreshToken con PK Long
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Consulta derivada: SELECT * FROM refresh_tokens WHERE token = ?
    // Busca un refresh token por su valor (cadena única). Retorna Optional porque puede no existir
    // Usado al renovar el access token: se valida que el refresh token existe y no está revocado
    Optional<RefreshToken> findByToken(String token);

    // @Modifying: necesario para queries que modifican datos (no solo consultan)
    // Indica a Spring Data JPA que esta query ejecuta un UPDATE, no un SELECT
    @Modifying
    // Consulta JPQL: UPDATE refresh_tokens SET revoked = true WHERE id_usuario = ?
    // Revoca (invalida) TODOS los refresh tokens de un usuario de una vez
    // Se usa al cerrar sesión o al cambiar la contraseña para invalidar todas las sesiones activas
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.usuario.id = :usuarioId")
    void revokeAllByUsuarioId(Long usuarioId);
}

package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRolNombreRol(String nombreRol);

    @Query("SELECT u FROM User u WHERE u.rol.nombreRol = 'ROLE_PACIENTE' AND u.estado = true " +
           "AND NOT EXISTS (SELECT p FROM Paciente p WHERE p.usuario.id = u.id AND p.fechaEliminacion IS NULL)")
    List<User> findPacientesSinActivar();
}

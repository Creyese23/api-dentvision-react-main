package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRolNombreRol(String nombreRol);

    List<User> findByRolNombreRolAndEstadoTrue(String nombreRol);

    List<User> findByRolNombreRolInAndEstadoTrue(List<String> roles);
}

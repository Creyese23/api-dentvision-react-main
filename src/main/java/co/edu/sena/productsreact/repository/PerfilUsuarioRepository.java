package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilUsuarioRepository extends JpaRepository<PerfilUsuario, Long> {
    boolean existsByIdentificacion(String identificacion);
    Optional<PerfilUsuario> findByIdentificacion(String identificacion);
}

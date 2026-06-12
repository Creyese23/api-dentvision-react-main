package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {
    List<Conversacion> findByPacienteId(Long pacienteId);
}

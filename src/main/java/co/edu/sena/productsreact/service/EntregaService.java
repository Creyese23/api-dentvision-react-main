package co.edu.sena.productsreact.service;

// Importación del DTO de respuesta de entrega
import co.edu.sena.productsreact.dto.entrega.EntregaResponse; // DTO con los datos de la entrega que se devuelven al cliente

// Entidades JPA que representan tablas en la BD
import co.edu.sena.productsreact.entity.Entrega;  // Entidad que representa la entrega de un trabajo de laboratorio al paciente
import co.edu.sena.productsreact.entity.Paciente; // Entidad del paciente, necesaria para filtrar entregas por paciente
import co.edu.sena.productsreact.entity.User;     // Entidad del usuario autenticado

// Excepción cuando no se encuentra un recurso solicitado
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Repositorios para acceder a las tablas de la BD
import co.edu.sena.productsreact.repository.EntregaRepository;   // Repositorio para gestionar entregas
import co.edu.sena.productsreact.repository.PacienteRepository;  // Repositorio para buscar el paciente vinculado al usuario
import co.edu.sena.productsreact.repository.UserRepository;      // Repositorio para buscar el usuario por email

import lombok.RequiredArgsConstructor; // Genera el constructor con inyección de dependencias
import org.springframework.stereotype.Service;                    // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones de escritura

import java.time.LocalDateTime; // Para registrar la fecha y hora exacta de la entrega
import java.util.List;          // Colección de elementos
import java.util.Optional;      // Contenedor que puede o no tener un valor (evita nulls)

// @Service: Spring registra esta clase como componente de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok inyecta los tres repositorios por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): la mayoría de operaciones aquí son de solo lectura
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class EntregaService {

    // Repositorio para consultar y actualizar entregas en la BD
    private final EntregaRepository entregaRepository;
    // Repositorio para buscar el usuario por su email (usado para identificar al paciente autenticado)
    private final UserRepository userRepository;
    // Repositorio para buscar el registro de paciente vinculado al usuario autenticado
    private final PacienteRepository pacienteRepository;

    /**
     * Lista TODAS las entregas del sistema.
     * Método para uso administrativo (dentistas, técnicos, admins).
     */
    public List<EntregaResponse> listar() {
        // Busca todas las entregas sin filtro y convierte cada entidad a su DTO
        return entregaRepository.findAll().stream().map(EntregaResponse::from).toList();
    }

    /**
     * Busca la entrega asociada a una orden específica.
     * Retorna Optional vacío si la orden no tiene entrega (aunque normalmente siempre la tiene).
     */
    public Optional<EntregaResponse> obtenerPorOrden(Long ordenId) {
        // Busca la entrega por el ID de la orden y la convierte a DTO si existe
        return entregaRepository.findByOrdenId(ordenId).map(EntregaResponse::from);
    }

    @Transactional // Esta operación escribe en la BD (UPDATE de estado)
    /**
     * Cambia el estado de una entrega (PENDIENTE → ENTREGADO, etc.).
     * Si el nuevo estado es ENTREGADO, registra automáticamente la fecha y hora de entrega.
     */
    public EntregaResponse cambiarEstado(Long id, String estado) {
        // Busca la entrega por ID; lanza excepción si no existe
        Entrega e = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega no encontrada"));
        e.setEstado(estado); // Actualiza el estado con el nuevo valor recibido
        // Si el estado es ENTREGADO, registra la fecha y hora exacta de la entrega al paciente
        if ("ENTREGADO".equals(estado)) e.setFechaEntrega(LocalDateTime.now());
        // Guarda los cambios y retorna el DTO actualizado
        return EntregaResponse.from(entregaRepository.save(e));
    }

    /**
     * Lista solo las entregas del paciente actualmente autenticado.
     * El paciente accede desde el frontend y solo ve sus propias entregas.
     * Se identifica al paciente a través del email del usuario autenticado.
     */
    public List<EntregaResponse> listarMisEntregas(String email) {
        // Busca el usuario en la BD por su email; lanza excepción si no existe
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        // Busca el registro de paciente vinculado a ese usuario usando su ID
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        // Busca todas las entregas que pertenecen a ese paciente y las convierte a DTOs
        return entregaRepository.findByPacienteId(paciente.getId())
                .stream().map(EntregaResponse::from).toList();
    }
}

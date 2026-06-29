package co.edu.sena.productsreact.service;

// Importaciones de DTOs para el sistema de mensajería interna
import co.edu.sena.productsreact.dto.comunicacion.MensajeRequest;  // DTO con los datos de un mensaje a enviar (conversación, remitente, contenido)
import co.edu.sena.productsreact.dto.comunicacion.MensajeResponse; // DTO con los datos del mensaje que se devuelven al cliente

// Importaciones de entidades JPA (importación comodín de todas las entidades del paquete)
import co.edu.sena.productsreact.entity.*;

// Excepción cuando no se encuentra un recurso solicitado
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Importaciones de repositorios (importación comodín de todos los repositorios)
import co.edu.sena.productsreact.repository.*;

import lombok.RequiredArgsConstructor; // Genera el constructor con inyección de dependencias
import org.springframework.stereotype.Service;                    // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones en operaciones de escritura

import java.time.LocalDateTime; // Para registrar la hora de inicio de conversaciones y mensajes
import java.util.List;          // Colección de elementos

// @Service: Spring registra esta clase como componente de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok inyecta los cinco repositorios por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): por defecto las operaciones son de solo lectura
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ConversacionService {

    // Repositorio para crear y consultar conversaciones (hilos de mensajes) en la BD
    private final ConversacionRepository conversacionRepository;
    // Repositorio para guardar y consultar mensajes individuales dentro de una conversación
    private final MensajeRepository mensajeRepository;
    // Repositorio para verificar que el paciente exista antes de iniciar una conversación
    private final PacienteRepository pacienteRepository;
    // Repositorio para asociar una conversación a un procedimiento específico (opcional)
    private final ProcedimientoRepository procedimientoRepository;
    // Repositorio para verificar que el remitente de un mensaje existe como usuario del sistema
    private final UserRepository userRepository;

    @Transactional // Esta operación crea un nuevo registro de conversación en la BD
    /**
     * Inicia una nueva conversación entre el paciente y el equipo de la clínica.
     * La conversación puede estar asociada opcionalmente a un procedimiento específico.
     * Retorna el ID de la conversación creada para que el cliente pueda enviar mensajes.
     */
    public Long iniciarConversacion(Long pacienteId, Long procedimientoId) {
        // Verifica que el paciente exista en la BD; lanza excepción si no se encuentra
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + pacienteId + " no encontrado"));
        // El procedimiento es opcional: si se envía un ID, se valida que exista
        Procedimiento proc = null;
        if (procedimientoId != null) {
            // Solo busca el procedimiento si se proporcionó su ID (puede ser null si es consulta general)
            proc = procedimientoRepository.findById(procedimientoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Procedimiento " + procedimientoId + " no encontrado"));
        }
        // Construye la entidad Conversacion con el paciente, el procedimiento (si aplica) y la hora de inicio
        Conversacion c = Conversacion.builder()
                .paciente(paciente)       // Paciente que inicia la conversación
                .procedimiento(proc)      // Procedimiento asociado (puede ser null para conversaciones generales)
                .inicio(LocalDateTime.now()) // Registra la fecha y hora en que inicia la conversación
                .build();
        // Guarda la conversación en la BD y retorna solo su ID (el cliente lo usa para enviar mensajes)
        return conversacionRepository.save(c).getId();
    }

    /**
     * Obtiene todos los mensajes de una conversación ordenados cronológicamente (del más antiguo al más nuevo).
     * Se usa para mostrar el historial de chat en la interfaz.
     */
    public List<MensajeResponse> mensajes(Long conversacionId) {
        // Busca todos los mensajes de la conversación ordenados por hora ascendente (primero el más antiguo)
        return mensajeRepository.findByConversacionIdOrderByHoraAsc(conversacionId)
                .stream().map(MensajeResponse::from).toList(); // Convierte cada Mensaje a su DTO
    }

    @Transactional // Esta operación inserta un nuevo mensaje en la BD
    /**
     * Envía un nuevo mensaje dentro de una conversación existente.
     * Registra el remitente, el contenido y la hora de envío.
     */
    public MensajeResponse enviarMensaje(MensajeRequest req) {
        // Verifica que la conversación donde se enviará el mensaje exista
        Conversacion conv = conversacionRepository.findById(req.idConversacion())
                .orElseThrow(() -> new ResourceNotFoundException("Conversación " + req.idConversacion() + " no encontrada"));
        // Verifica que el usuario remitente exista en el sistema
        User remitente = userRepository.findById(req.idRemitente())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + req.idRemitente() + " no encontrado"));

        // Construye la entidad Mensaje con todos los datos necesarios
        Mensaje m = Mensaje.builder()
                .conversacion(conv)            // Conversación a la que pertenece este mensaje
                .remitente(remitente)          // Usuario que envía el mensaje (puede ser paciente o personal de la clínica)
                .contenido(req.contenido())    // Texto del mensaje enviado
                .hora(LocalDateTime.now())     // Hora exacta de envío del mensaje
                .estado("ENVIADO").build();    // Estado inicial del mensaje: ENVIADO (podría cambiar a LEIDO)
        // Guarda el mensaje en la BD y retorna su DTO con todos los datos
        return MensajeResponse.from(mensajeRepository.save(m));
    }

    @Transactional // Esta operación actualiza la conversación en la BD
    /**
     * Cierra una conversación registrando la fecha y hora de finalización.
     * Una conversación cerrada indica que el asunto fue resuelto.
     */
    public void cerrarConversacion(Long id) {
        // Busca la conversación por ID; lanza excepción si no existe
        Conversacion c = conversacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación " + id + " no encontrada"));
        c.setFin(LocalDateTime.now()); // Registra la fecha y hora en que se cierra la conversación
        conversacionRepository.save(c); // Persiste el cierre en la BD
    }
}

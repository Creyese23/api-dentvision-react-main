package co.edu.sena.productsreact.service;

// Importaciones de DTOs para las citas odontológicas
import co.edu.sena.productsreact.dto.cita.AgendarCitaRequest; // DTO para agendar una cita desde el portal del paciente (idOdontologo, fechaHora, motivo)
import co.edu.sena.productsreact.dto.cita.CitaRequest;        // DTO para crear una cita desde el panel administrativo
import co.edu.sena.productsreact.dto.cita.CitaResponse;       // DTO con los datos de la cita que se devuelven al cliente

// Entidades JPA que representan tablas en la BD
import co.edu.sena.productsreact.entity.Cita;     // Entidad de la cita odontológica
import co.edu.sena.productsreact.entity.Empleado; // Entidad del empleado que atiende la cita (odontólogo)
import co.edu.sena.productsreact.entity.Notificacion; // Entidad de notificación in-app
import co.edu.sena.productsreact.entity.Paciente; // Entidad del paciente que asiste a la cita
import co.edu.sena.productsreact.entity.User;     // Entidad del usuario autenticado

// Excepción cuando no se encuentra el recurso solicitado
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Repositorios para acceder a las tablas en la BD
import co.edu.sena.productsreact.repository.CitaRepository;      // Repositorio para gestionar citas
import co.edu.sena.productsreact.repository.EmpleadoRepository;  // Repositorio para buscar al odontólogo
import co.edu.sena.productsreact.repository.NotificacionRepository; // Repositorio para guardar notificaciones in-app
import co.edu.sena.productsreact.repository.PacienteRepository;  // Repositorio para buscar al paciente
import co.edu.sena.productsreact.repository.UserRepository;      // Repositorio para buscar el usuario por email

import lombok.RequiredArgsConstructor;        // Genera el constructor con inyección de dependencias
import lombok.extern.slf4j.Slf4j;            // Genera el logger para registrar advertencias en consola/log
import org.springframework.stereotype.Service; // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones de escritura

import java.time.LocalDate;           // Para trabajar con fechas sin hora (para buscar disponibilidad por día)
import java.time.LocalDateTime;       // Para registrar fechas y horas de las citas
import java.time.format.DateTimeFormatter; // Para formatear fechas en los correos de notificación
import java.util.List;                // Colección de elementos

// @Service: Spring registra esta clase como componente de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok inyecta los cinco repositorios y el servicio de email por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): las consultas son de solo lectura por defecto
@Transactional(readOnly = true)
// @Slf4j: Lombok genera automáticamente el objeto "log" para escribir mensajes en el log del servidor
@Slf4j
public class CitaService {

    // Formateador de fecha y hora para los emails de confirmación (ej: "28/06/2026 10:30")
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // Formateador para mensajes de notificación in-app
    private static final DateTimeFormatter FMT_NOTIF = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    // Lista de horarios laborales disponibles para agendar citas
    // Horario de mañana: 8:00 a 11:30 (cada 30 minutos) y de tarde: 14:00 a 17:00
    private static final List<String> HORAS_LABORALES = List.of(
            "08:00","08:30","09:00","09:30","10:00","10:30","11:00","11:30",
            "14:00","14:30","15:00","15:30","16:00","16:30","17:00"
    );

    // Repositorio para crear, buscar y actualizar citas en la BD
    private final CitaRepository citaRepository;
    // Repositorio para buscar el paciente que agenda la cita
    private final PacienteRepository pacienteRepository;
    // Repositorio para buscar el odontólogo asignado a la cita
    private final EmpleadoRepository empleadoRepository;
    // Repositorio para buscar el usuario autenticado por su email
    private final UserRepository userRepository;
    // Servicio de email para enviar confirmaciones y cancelaciones al paciente
    private final EmailService emailService;
    // Repositorio para guardar notificaciones in-app
    private final NotificacionRepository notificacionRepository;

    /**
     * Lista todas las citas del paciente autenticado (el que está logueado).
     * El paciente solo puede ver sus propias citas, no las de otros pacientes.
     */
    public List<CitaResponse> listarMisCitas(String email) {
        // Busca el usuario en la BD por su email del token JWT
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        // Busca el registro de paciente vinculado a ese usuario
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        // Busca las citas del paciente que no han sido eliminadas lógicamente
        return citaRepository.findByPacienteIdAndFechaEliminacionIsNull(paciente.getId())
                .stream().map(CitaResponse::from).toList();
    }

    /**
     * Lista todas las citas de un paciente específico por su ID.
     * Usado por el personal de la clínica para ver el historial de un paciente.
     */
    public List<CitaResponse> listarPorPaciente(Long pacienteId) {
        // Busca citas del paciente que no tienen fecha de eliminación (no canceladas con borrado lógico)
        return citaRepository.findByPacienteIdAndFechaEliminacionIsNull(pacienteId)
                .stream().map(CitaResponse::from).toList();
    }

    /**
     * Lista todas las citas asignadas a un odontólogo específico.
     * El odontólogo puede ver su agenda desde el panel de empleados.
     */
    public List<CitaResponse> listarPorOdontologo(Long odontologoId) {
        // Busca las citas del odontólogo excluyendo las eliminadas lógicamente
        return citaRepository.findByOdontologoIdAndFechaEliminacionIsNull(odontologoId)
                .stream().map(CitaResponse::from).toList();
    }

    /**
     * Lista todas las citas programadas dentro de un rango de fechas.
     * Útil para generar reportes o calendarios de trabajo.
     */
    public List<CitaResponse> listarPorRangoFecha(LocalDateTime inicio, LocalDateTime fin) {
        // Consulta personalizada en el repositorio que filtra por rango de fecha
        return citaRepository.findByRangoFecha(inicio, fin)
                .stream().map(CitaResponse::from).toList();
    }

    /**
     * Obtiene una cita específica por su ID.
     * Lanza excepción si no existe.
     */
    public CitaResponse obtener(Long id) {
        // getOrThrow verifica existencia y lanza ResourceNotFoundException si no existe
        return CitaResponse.from(getOrThrow(id));
    }

    /** Horarios disponibles de un odontólogo para una fecha específica */
    /**
     * Calcula los horarios disponibles de un odontólogo para un día dado.
     * Compara la lista de horas laborales con las ya ocupadas y retorna las libres.
     */
    public List<String> horasDisponibles(Long odontologoId, LocalDate fecha) {
        // Convierte la fecha a rango de datetime para buscar citas de todo ese día
        LocalDateTime inicio = fecha.atStartOfDay();          // Inicio del día: 00:00:00
        LocalDateTime fin    = fecha.atTime(23, 59, 59);      // Fin del día: 23:59:59

        // Busca las citas del odontólogo en ese rango de fecha y extrae las horas ocupadas
        List<String> ocupadas = citaRepository.findByOdontologoAndRangoFecha(odontologoId, inicio, fin)
                .stream()
                .filter(c -> !"CANCELADA".equals(c.getEstado())) // Excluye citas canceladas (su hora vuelve a estar disponible)
                .map(c -> c.getFechaHora().toLocalTime().toString().substring(0, 5)) // Extrae solo HH:mm de la fecha-hora
                .toList();

        // Retorna solo las horas laborales que NO están en la lista de ocupadas
        return HORAS_LABORALES.stream().filter(h -> !ocupadas.contains(h)).toList();
    }

    @Transactional // Esta operación crea una cita en la BD y envía un email
    /**
     * Permite al paciente autenticado agendar su propia cita.
     * Valida que el horario esté disponible y envía confirmación por email.
     */
    public CitaResponse agendarCita(String email, AgendarCitaRequest req) {
        // Busca el usuario por email (del token JWT)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        // Busca el perfil de paciente vinculado al usuario
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes un perfil de paciente registrado"));
        // Busca el odontólogo que el paciente seleccionó
        Empleado odontologo = empleadoRepository.findById(req.idOdontologo())
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));

        // Verifica si el odontólogo ya tiene una cita NO cancelada en la misma fecha y hora
        // para evitar doble reserva del mismo horario
        boolean ocupado = citaRepository.findByOdontologoIdAndFechaEliminacionIsNull(req.idOdontologo())
                .stream().anyMatch(c -> c.getFechaHora().equals(req.fechaHora()) && !"CANCELADA".equals(c.getEstado()));
        // Si el horario ya está ocupado, lanza excepción con mensaje claro para el paciente
        if (ocupado) throw new IllegalArgumentException("El horario seleccionado ya está ocupado");

        // Construye y guarda la cita con estado PROGRAMADA
        Cita cita = Cita.builder()
                .paciente(paciente)           // Paciente que asistirá a la cita
                .odontologo(odontologo)       // Odontólogo que atenderá la cita
                .fechaHora(req.fechaHora())   // Fecha y hora de la cita
                .motivo(req.motivo())         // Motivo de la consulta (ej. "Revisión general", "Dolor de muela")
                .estado("PROGRAMADA").build(); // Estado inicial: la cita está programada
        CitaResponse resp = CitaResponse.from(citaRepository.save(cita)); // Guarda y convierte a DTO
        // Intenta enviar email de confirmación al paciente; si falla, solo registra advertencia en el log
        try {
            String nombrePaciente = paciente.getUsuario().getPerfil().getNombres(); // Nombre del paciente para el email
            // Construye el nombre completo del odontólogo para incluirlo en el email
            String nombreOdontologo = odontologo.getUsuario().getPerfil().getNombres()
                + " " + odontologo.getUsuario().getPerfil().getApellidos();
            // Envía el email de confirmación con los detalles de la cita
            emailService.sendCitaConfirmada(
                email, nombrePaciente, req.motivo(),
                req.fechaHora().format(FMT), nombreOdontologo);
        } catch (Exception e) {
            // Si el email falla (servidor caído, datos incorrectos, etc.), no interrumpe el flujo principal
            log.warn("No se pudo enviar email de confirmación de cita: {}", e.getMessage());
        }
        return resp;
    }

    /** El paciente cancela su propia cita */
    @Transactional // Esta operación actualiza la cita y envía un email de cancelación
    /**
     * Permite que el paciente autenticado cancele una de sus propias citas.
     * Verifica que la cita le pertenezca y que no esté ya cancelada.
     */
    public CitaResponse cancelarPorPaciente(Long citaId, String email) {
        // Busca el usuario autenticado por email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        // Busca el perfil de paciente del usuario autenticado
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        // Obtiene la cita que se desea cancelar
        Cita cita = getOrThrow(citaId);

        // Verifica que la cita pertenezca al paciente autenticado (seguridad: evita que un paciente cancele citas de otro)
        if (!cita.getPaciente().getId().equals(paciente.getId()))
            throw new IllegalArgumentException("No tienes permiso para cancelar esta cita");
        // Verifica que la cita no esté ya cancelada (no tiene sentido cancelar algo ya cancelado)
        if ("CANCELADA".equals(cita.getEstado()))
            throw new IllegalArgumentException("La cita ya está cancelada");

        cita.setEstado("CANCELADA");                              // Marca la cita como cancelada
        cita.setFechaEliminacion(LocalDateTime.now());            // Registra la fecha y hora de cancelación
        CitaResponse resp = CitaResponse.from(citaRepository.save(cita)); // Guarda los cambios
        // Intenta enviar email de cancelación; si falla, solo registra advertencia en el log
        try {
            String nombrePaciente = paciente.getUsuario().getPerfil().getNombres();
            // Envía notificación de cancelación con el motivo y fecha de la cita cancelada
            emailService.sendCitaCancelada(
                email, nombrePaciente, cita.getMotivo(),
                cita.getFechaHora().format(FMT));
        } catch (Exception e) {
            log.warn("No se pudo enviar email de cancelación: {}", e.getMessage());
        }
        // Notificaciones in-app
        try {
            String fechaFmt = cita.getFechaHora().format(FMT_NOTIF);
            User userOdontologo = cita.getOdontologo() != null ? cita.getOdontologo().getUsuario() : null;
            if (userOdontologo != null) {
                notificacionRepository.save(Notificacion.builder()
                        .usuario(userOdontologo)
                        .tipo("CITA")
                        .mensaje("La cita del " + fechaFmt + " fue cancelada por el paciente")
                        .build());
            }
        } catch (Exception e) {
            log.warn("No se pudo guardar notificación de cancelación al odontólogo: {}", e.getMessage());
        }
        try {
            String fechaFmt = cita.getFechaHora().format(FMT_NOTIF);
            User userPaciente = paciente.getUsuario();
            if (userPaciente != null) {
                notificacionRepository.save(Notificacion.builder()
                        .usuario(userPaciente)
                        .tipo("CITA")
                        .mensaje("Tu cita del " + fechaFmt + " ha sido cancelada")
                        .build());
            }
        } catch (Exception e) {
            log.warn("No se pudo guardar notificación de cancelación al paciente: {}", e.getMessage());
        }
        return resp;
    }

    /** El paciente reagenda su propia cita (cambia fecha/hora y/u odontólogo) */
    @Transactional // Esta operación actualiza la cita en la BD
    /**
     * Permite al paciente autenticado cambiar la fecha/hora y/o el odontólogo de su cita.
     * Valida permisos, que la cita no esté cancelada y que el nuevo horario esté disponible.
     */
    public CitaResponse reagendarPorPaciente(Long citaId, String email, AgendarCitaRequest req) {
        // Busca el usuario autenticado y su perfil de paciente
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Cita cita = getOrThrow(citaId); // Obtiene la cita que se quiere reagendar

        // Verifica que la cita pertenece al paciente autenticado
        if (!cita.getPaciente().getId().equals(paciente.getId()))
            throw new IllegalArgumentException("No tienes permiso para modificar esta cita");
        // No se puede reagendar una cita ya cancelada
        if ("CANCELADA".equals(cita.getEstado()))
            throw new IllegalArgumentException("No puedes reagendar una cita cancelada");

        // Verifica que el nuevo horario del odontólogo esté disponible
        // Se excluye la propia cita actual (!c.getId().equals(citaId)) para no bloquearse a sí misma
        boolean ocupado = citaRepository.findByOdontologoIdAndFechaEliminacionIsNull(req.idOdontologo())
                .stream().anyMatch(c -> !c.getId().equals(citaId)  // Excluye la cita actual de la comparación
                        && c.getFechaHora().equals(req.fechaHora()) // Compara la nueva fecha/hora con las existentes
                        && !"CANCELADA".equals(c.getEstado()));     // Ignora citas canceladas
        if (ocupado) throw new IllegalArgumentException("El horario seleccionado ya está ocupado. Elige otro.");

        // Busca el odontólogo nuevo (puede ser el mismo u otro)
        Empleado odontologo = empleadoRepository.findById(req.idOdontologo())
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));

        cita.setOdontologo(odontologo);        // Actualiza el odontólogo (puede ser diferente al original)
        cita.setFechaHora(req.fechaHora());    // Actualiza la fecha y hora de la cita
        // Solo actualiza el motivo si el request trae uno nuevo (no obligatorio al reagendar)
        if (req.motivo() != null) cita.setMotivo(req.motivo());
        // Guarda la cita reagendada y retorna el DTO actualizado
        return CitaResponse.from(citaRepository.save(cita));
    }

    @Transactional // Esta operación crea una cita desde el panel administrativo
    /**
     * Crea una cita directamente desde el panel administrativo.
     * A diferencia de agendarCita, no valida disponibilidad y no envía email.
     */
    public CitaResponse crear(CitaRequest req) {
        // Busca el paciente por el ID que viene en el request administrativo
        Paciente paciente = pacienteRepository.findById(req.idPaciente())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + req.idPaciente() + " no encontrado"));
        // Busca el odontólogo por el ID del request
        Empleado odontologo = empleadoRepository.findById(req.idOdontologo())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado " + req.idOdontologo() + " no encontrado"));
        // Construye y guarda la cita con estado PROGRAMADA
        Cita cita = Cita.builder()
                .paciente(paciente).odontologo(odontologo)
                .fechaHora(req.fechaHora()).motivo(req.motivo())
                .estado("PROGRAMADA").build();
        CitaResponse resp = CitaResponse.from(citaRepository.save(cita));

        // Notificaciones in-app al paciente y al odontólogo
        try {
            String fechaFmt = req.fechaHora().format(FMT_NOTIF);
            User userPaciente = paciente.getUsuario();
            if (userPaciente != null) {
                notificacionRepository.save(Notificacion.builder()
                        .usuario(userPaciente)
                        .tipo("CITA")
                        .mensaje("Nueva cita agendada el " + fechaFmt)
                        .build());
            }
        } catch (Exception e) {
            log.warn("No se pudo guardar notificación de cita al paciente: {}", e.getMessage());
        }
        try {
            User userOdontologo = odontologo.getUsuario();
            String nombrePaciente = paciente.getUsuario() != null && paciente.getUsuario().getPerfil() != null
                    ? paciente.getUsuario().getPerfil().getNombres() : "paciente";
            if (userOdontologo != null) {
                notificacionRepository.save(Notificacion.builder()
                        .usuario(userOdontologo)
                        .tipo("CITA")
                        .mensaje("Nueva cita asignada: paciente " + nombrePaciente + " el " + req.fechaHora().format(FMT_NOTIF))
                        .build());
            }
        } catch (Exception e) {
            log.warn("No se pudo guardar notificación de cita al odontólogo: {}", e.getMessage());
        }

        return resp;
    }

    @Transactional // Esta operación actualiza el estado de la cita y puede enviar emails
    /**
     * Cambia el estado de una cita (PROGRAMADA → CONFIRMADA → ATENDIDA → CANCELADA).
     * Envía notificación por email al paciente cuando se confirma o cancela la cita.
     */
    public CitaResponse cambiarEstado(Long id, String estado) {
        Cita cita = getOrThrow(id); // Obtiene la cita; lanza excepción si no existe
        cita.setEstado(estado);     // Actualiza el estado con el nuevo valor
        CitaResponse resp = CitaResponse.from(citaRepository.save(cita)); // Persiste y convierte a DTO
        // Intenta enviar email de notificación al paciente según el nuevo estado
        try {
            String email = cita.getPaciente().getUsuario().getEmail();          // Email del paciente para notificarlo
            String nombre = cita.getPaciente().getUsuario().getPerfil().getNombres(); // Nombre del paciente para el saludo
            String fechaHora = cita.getFechaHora().format(FMT);                // Fecha y hora formateada para el email
            // Nombre completo del odontólogo para incluir en la notificación
            String odontologo = cita.getOdontologo().getUsuario().getPerfil().getNombres()
                    + " " + cita.getOdontologo().getUsuario().getPerfil().getApellidos();
            if ("CONFIRMADA".equals(estado)) {
                // Envía email de confirmación cuando el personal de la clínica confirma la cita
                emailService.sendCitaConfirmada(email, nombre, cita.getMotivo(), fechaHora, odontologo);
            } else if ("CANCELADA".equals(estado)) {
                // Envía email de cancelación cuando el personal cancela la cita desde el panel admin
                emailService.sendCitaCancelada(email, nombre, cita.getMotivo(), fechaHora);
            }
        } catch (Exception e) {
            // Si el email falla, no se interrumpe el cambio de estado; se registra como advertencia
            log.warn("No se pudo enviar email de cambio de estado de cita: {}", e.getMessage());
        }
        // Notificaciones in-app al paciente según el nuevo estado
        try {
            User userPaciente = cita.getPaciente() != null ? cita.getPaciente().getUsuario() : null;
            if (userPaciente != null) {
                String fechaFmt = cita.getFechaHora().format(FMT_NOTIF);
                if ("CONFIRMADA".equals(estado)) {
                    notificacionRepository.save(Notificacion.builder()
                            .usuario(userPaciente)
                            .tipo("CITA")
                            .mensaje("Tu cita del " + fechaFmt + " ha sido confirmada")
                            .build());
                } else if ("CANCELADA".equals(estado)) {
                    notificacionRepository.save(Notificacion.builder()
                            .usuario(userPaciente)
                            .tipo("CITA")
                            .mensaje("Tu cita del " + fechaFmt + " fue cancelada")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo guardar notificación de cambio de estado de cita: {}", e.getMessage());
        }
        return resp;
    }

    @Transactional // Esta operación actualiza datos de la cita en la BD
    /**
     * Actualiza todos los datos de una cita existente (paciente, odontólogo, fecha, motivo).
     * Usado por el administrador para correcciones.
     */
    public CitaResponse actualizar(Long id, CitaRequest req) {
        Cita cita = getOrThrow(id); // Obtiene la cita existente
        // Busca el nuevo paciente; puede ser diferente al original si se cambió de paciente
        Paciente paciente = pacienteRepository.findById(req.idPaciente())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + req.idPaciente() + " no encontrado"));
        // Busca el nuevo odontólogo asignado
        Empleado odontologo = empleadoRepository.findById(req.idOdontologo())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado " + req.idOdontologo() + " no encontrado"));
        cita.setPaciente(paciente);          // Actualiza el paciente (en caso de corrección)
        cita.setOdontologo(odontologo);      // Actualiza el odontólogo asignado
        cita.setFechaHora(req.fechaHora());  // Actualiza la fecha y hora de la cita
        cita.setMotivo(req.motivo());        // Actualiza el motivo de la consulta
        // Guarda los cambios y retorna el DTO actualizado
        return CitaResponse.from(citaRepository.save(cita));
    }

    @Transactional // Esta operación hace eliminación lógica de la cita
    /**
     * Elimina lógicamente una cita (la marca como CANCELADA y registra la fecha de eliminación).
     * También envía email de cancelación al paciente.
     */
    public void eliminar(Long id) {
        Cita cita = getOrThrow(id); // Obtiene la cita; lanza excepción si no existe
        cita.setFechaEliminacion(LocalDateTime.now()); // Registra la fecha de eliminación lógica
        cita.setEstado("CANCELADA");                   // Marca la cita como cancelada
        citaRepository.save(cita);                     // Persiste los cambios en la BD
        // Intenta enviar email de cancelación al paciente después de la eliminación
        try {
            String email = cita.getPaciente().getUsuario().getEmail();
            String nombrePaciente = cita.getPaciente().getUsuario().getPerfil().getNombres();
            // Envía el email de cancelación con el motivo y fecha original de la cita
            emailService.sendCitaCancelada(
                email, nombrePaciente, cita.getMotivo(),
                cita.getFechaHora().format(FMT));
        } catch (Exception e) {
            // Si el email falla, no interrumpe la eliminación; se registra como advertencia
            log.warn("No se pudo enviar email de cancelación (admin): {}", e.getMessage());
        }
    }

    /**
     * Método auxiliar privado: busca una cita por ID y lanza excepción descriptiva si no existe.
     * Se usa en todos los métodos que necesitan verificar que la cita exista antes de operar.
     */
    private Cita getOrThrow(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita " + id + " no encontrada"));
    }
}

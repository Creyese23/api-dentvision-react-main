package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.cita.AgendarCitaRequest;
import co.edu.sena.productsreact.dto.cita.CitaRequest;
import co.edu.sena.productsreact.dto.cita.CitaResponse;
import co.edu.sena.productsreact.entity.Cita;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.CitaRepository;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CitaService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<String> HORAS_LABORALES = List.of(
            "08:00","08:30","09:00","09:30","10:00","10:30","11:00","11:30",
            "14:00","14:30","15:00","15:30","16:00","16:30","17:00"
    );

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public List<CitaResponse> listarMisCitas(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        return citaRepository.findByPacienteIdAndFechaEliminacionIsNull(paciente.getId())
                .stream().map(CitaResponse::from).toList();
    }

    public List<CitaResponse> listarPorPaciente(Long pacienteId) {
        return citaRepository.findByPacienteIdAndFechaEliminacionIsNull(pacienteId)
                .stream().map(CitaResponse::from).toList();
    }

    public List<CitaResponse> listarPorOdontologo(Long odontologoId) {
        return citaRepository.findByOdontologoIdAndFechaEliminacionIsNull(odontologoId)
                .stream().map(CitaResponse::from).toList();
    }

    public List<CitaResponse> listarPorRangoFecha(LocalDateTime inicio, LocalDateTime fin) {
        return citaRepository.findByRangoFecha(inicio, fin)
                .stream().map(CitaResponse::from).toList();
    }

    public CitaResponse obtener(Long id) {
        return CitaResponse.from(getOrThrow(id));
    }

    /** Horarios disponibles de un odontólogo para una fecha específica */
    public List<String> horasDisponibles(Long odontologoId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin    = fecha.atTime(23, 59, 59);

        List<String> ocupadas = citaRepository.findByOdontologoAndRangoFecha(odontologoId, inicio, fin)
                .stream()
                .filter(c -> !"CANCELADA".equals(c.getEstado()))
                .map(c -> c.getFechaHora().toLocalTime().toString().substring(0, 5))
                .toList();

        return HORAS_LABORALES.stream().filter(h -> !ocupadas.contains(h)).toList();
    }

    @Transactional
    public CitaResponse agendarCita(String email, AgendarCitaRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes un perfil de paciente registrado"));
        Empleado odontologo = empleadoRepository.findById(req.idOdontologo())
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));

        boolean ocupado = citaRepository.findByOdontologoIdAndFechaEliminacionIsNull(req.idOdontologo())
                .stream().anyMatch(c -> c.getFechaHora().equals(req.fechaHora()) && !"CANCELADA".equals(c.getEstado()));
        if (ocupado) throw new IllegalArgumentException("El horario seleccionado ya está ocupado");

        Cita cita = Cita.builder()
                .paciente(paciente).odontologo(odontologo)
                .fechaHora(req.fechaHora()).motivo(req.motivo())
                .estado("PROGRAMADA").build();
        CitaResponse resp = CitaResponse.from(citaRepository.save(cita));
        try {
            String nombrePaciente = paciente.getUsuario().getPerfil().getNombres();
            String nombreOdontologo = odontologo.getUsuario().getPerfil().getNombres()
                + " " + odontologo.getUsuario().getPerfil().getApellidos();
            emailService.sendCitaConfirmada(
                email, nombrePaciente, req.motivo(),
                req.fechaHora().format(FMT), nombreOdontologo);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de confirmación de cita: {}", e.getMessage());
        }
        return resp;
    }

    /** El paciente cancela su propia cita */
    @Transactional
    public CitaResponse cancelarPorPaciente(Long citaId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Cita cita = getOrThrow(citaId);

        if (!cita.getPaciente().getId().equals(paciente.getId()))
            throw new IllegalArgumentException("No tienes permiso para cancelar esta cita");
        if ("CANCELADA".equals(cita.getEstado()))
            throw new IllegalArgumentException("La cita ya está cancelada");

        cita.setEstado("CANCELADA");
        cita.setFechaEliminacion(LocalDateTime.now());
        CitaResponse resp = CitaResponse.from(citaRepository.save(cita));
        try {
            String nombrePaciente = paciente.getUsuario().getPerfil().getNombres();
            emailService.sendCitaCancelada(
                email, nombrePaciente, cita.getMotivo(),
                cita.getFechaHora().format(FMT));
        } catch (Exception e) {
            log.warn("No se pudo enviar email de cancelación: {}", e.getMessage());
        }
        return resp;
    }

    /** El paciente reagenda su propia cita (cambia fecha/hora y/u odontólogo) */
    @Transactional
    public CitaResponse reagendarPorPaciente(Long citaId, String email, AgendarCitaRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Cita cita = getOrThrow(citaId);

        if (!cita.getPaciente().getId().equals(paciente.getId()))
            throw new IllegalArgumentException("No tienes permiso para modificar esta cita");
        if ("CANCELADA".equals(cita.getEstado()))
            throw new IllegalArgumentException("No puedes reagendar una cita cancelada");

        boolean ocupado = citaRepository.findByOdontologoIdAndFechaEliminacionIsNull(req.idOdontologo())
                .stream().anyMatch(c -> !c.getId().equals(citaId)
                        && c.getFechaHora().equals(req.fechaHora())
                        && !"CANCELADA".equals(c.getEstado()));
        if (ocupado) throw new IllegalArgumentException("El horario seleccionado ya está ocupado. Elige otro.");

        Empleado odontologo = empleadoRepository.findById(req.idOdontologo())
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));

        cita.setOdontologo(odontologo);
        cita.setFechaHora(req.fechaHora());
        if (req.motivo() != null) cita.setMotivo(req.motivo());
        return CitaResponse.from(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponse crear(CitaRequest req) {
        Paciente paciente = pacienteRepository.findById(req.idPaciente())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + req.idPaciente() + " no encontrado"));
        Empleado odontologo = empleadoRepository.findById(req.idOdontologo())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado " + req.idOdontologo() + " no encontrado"));
        Cita cita = Cita.builder()
                .paciente(paciente).odontologo(odontologo)
                .fechaHora(req.fechaHora()).motivo(req.motivo())
                .estado("PROGRAMADA").build();
        return CitaResponse.from(citaRepository.save(cita));
    }

    @Transactional
    public CitaResponse cambiarEstado(Long id, String estado) {
        Cita cita = getOrThrow(id);
        cita.setEstado(estado);
        CitaResponse resp = CitaResponse.from(citaRepository.save(cita));
        try {
            String email = cita.getPaciente().getUsuario().getEmail();
            String nombre = cita.getPaciente().getUsuario().getPerfil().getNombres();
            String fechaHora = cita.getFechaHora().format(FMT);
            String odontologo = cita.getOdontologo().getUsuario().getPerfil().getNombres()
                    + " " + cita.getOdontologo().getUsuario().getPerfil().getApellidos();
            if ("CONFIRMADA".equals(estado)) {
                emailService.sendCitaConfirmada(email, nombre, cita.getMotivo(), fechaHora, odontologo);
            } else if ("CANCELADA".equals(estado)) {
                emailService.sendCitaCancelada(email, nombre, cita.getMotivo(), fechaHora);
            }
        } catch (Exception e) {
            log.warn("No se pudo enviar email de cambio de estado de cita: {}", e.getMessage());
        }
        return resp;
    }

    @Transactional
    public CitaResponse actualizar(Long id, CitaRequest req) {
        Cita cita = getOrThrow(id);
        Paciente paciente = pacienteRepository.findById(req.idPaciente())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + req.idPaciente() + " no encontrado"));
        Empleado odontologo = empleadoRepository.findById(req.idOdontologo())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado " + req.idOdontologo() + " no encontrado"));
        cita.setPaciente(paciente);
        cita.setOdontologo(odontologo);
        cita.setFechaHora(req.fechaHora());
        cita.setMotivo(req.motivo());
        return CitaResponse.from(citaRepository.save(cita));
    }

    @Transactional
    public void eliminar(Long id) {
        Cita cita = getOrThrow(id);
        cita.setFechaEliminacion(LocalDateTime.now());
        cita.setEstado("CANCELADA");
        citaRepository.save(cita);
        try {
            String email = cita.getPaciente().getUsuario().getEmail();
            String nombrePaciente = cita.getPaciente().getUsuario().getPerfil().getNombres();
            emailService.sendCitaCancelada(
                email, nombrePaciente, cita.getMotivo(),
                cita.getFechaHora().format(FMT));
        } catch (Exception e) {
            log.warn("No se pudo enviar email de cancelación (admin): {}", e.getMessage());
        }
    }

    private Cita getOrThrow(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita " + id + " no encontrada"));
    }
}

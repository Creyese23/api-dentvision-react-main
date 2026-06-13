package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.cita.AgendarCitaRequest;
import co.edu.sena.productsreact.dto.cita.CitaRequest;
import co.edu.sena.productsreact.dto.cita.CitaResponse;
import co.edu.sena.productsreact.entity.Cita;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.repository.CitaRepository;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UserRepository userRepository;

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
        return CitaResponse.from(citaRepository.save(cita));
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
    }

    private Cita getOrThrow(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita " + id + " no encontrada"));
    }
}

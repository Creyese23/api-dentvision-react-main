package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.cita.CitaRequest;
import co.edu.sena.productsreact.dto.cita.CitaResponse;
import co.edu.sena.productsreact.entity.Cita;
import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.CitaRepository;
import co.edu.sena.productsreact.repository.EmpleadoRepository;
import co.edu.sena.productsreact.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final EmpleadoRepository empleadoRepository;

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

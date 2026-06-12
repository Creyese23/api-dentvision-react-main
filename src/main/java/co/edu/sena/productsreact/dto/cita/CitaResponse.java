package co.edu.sena.productsreact.dto.cita;

import co.edu.sena.productsreact.entity.Cita;

import java.time.LocalDateTime;

public record CitaResponse(
        Long id,
        Long idPaciente,
        String nombrePaciente,
        Long idOdontologo,
        String nombreOdontologo,
        LocalDateTime fechaHora,
        String motivo,
        String estado
) {
    public static CitaResponse from(Cita c) {
        String paciente = c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos();
        String odontologo = c.getOdontologo().getNombres() + " " + c.getOdontologo().getApellidos();
        return new CitaResponse(
                c.getId(), c.getPaciente().getId(), paciente,
                c.getOdontologo().getId(), odontologo,
                c.getFechaHora(), c.getMotivo(), c.getEstado()
        );
    }
}

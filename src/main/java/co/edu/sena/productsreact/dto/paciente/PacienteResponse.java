package co.edu.sena.productsreact.dto.paciente;

import co.edu.sena.productsreact.entity.Paciente;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PacienteResponse(
        Long id,
        String nombres,
        String apellidos,
        String documento,
        String tipoIdentificacion,
        String telefono,
        String genero,
        LocalDate fechaNacimiento,
        Boolean estado,
        LocalDateTime fechaCreacion
) {
    public static PacienteResponse from(Paciente p) {
        return new PacienteResponse(
                p.getId(), p.getNombres(), p.getApellidos(),
                p.getDocumento(), p.getTipoIdentificacion(),
                p.getTelefono(), p.getGenero(), p.getFechaNacimiento(),
                p.getEstado(), p.getFechaCreacion()
        );
    }
}

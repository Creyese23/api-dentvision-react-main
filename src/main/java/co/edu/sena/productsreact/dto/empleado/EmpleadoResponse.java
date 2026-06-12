package co.edu.sena.productsreact.dto.empleado;

import co.edu.sena.productsreact.entity.Empleado;

import java.time.LocalDateTime;

public record EmpleadoResponse(
        Long id,
        String nombres,
        String apellidos,
        String documento,
        String tipoIdentificacion,
        String telefono,
        String cargo,
        Boolean estado,
        LocalDateTime fechaCreacion
) {
    public static EmpleadoResponse from(Empleado e) {
        return new EmpleadoResponse(
                e.getId(), e.getNombres(), e.getApellidos(),
                e.getDocumento(), e.getTipoIdentificacion(),
                e.getTelefono(), e.getCargo(),
                e.getEstado(), e.getFechaCreacion()
        );
    }
}

package co.edu.sena.productsreact.dto.factura;

import co.edu.sena.productsreact.entity.Empleado;
import co.edu.sena.productsreact.entity.Factura;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaResponse(
        Long id,
        Long idPaciente,
        String nombrePaciente,
        Long idOdontologo,
        String nombreOdontologo,
        LocalDateTime fechaEmision,
        BigDecimal total,
        String estado,
        List<DetalleFacturaResponse> detalles
) {
    public static FacturaResponse from(Factura f) {
        List<DetalleFacturaResponse> detalles = f.getDetalles() != null
                ? f.getDetalles().stream().map(DetalleFacturaResponse::from).toList()
                : List.of();

        return new FacturaResponse(
                f.getId(),
                f.getPaciente().getId(), nombreCompleto(f.getPaciente()),
                f.getOdontologo() != null ? f.getOdontologo().getId() : null,
                f.getOdontologo() != null ? nombreCompletoEmpleado(f.getOdontologo()) : null,
                f.getFechaEmision(), f.getTotal(), f.getEstado(),
                detalles
        );
    }

    private static String nombreCompleto(Paciente p) {
        PerfilUsuario perfil = p.getUsuario() != null ? p.getUsuario().getPerfil() : null;
        if (perfil == null) return "";
        return perfil.getNombres() + " " + perfil.getApellidos();
    }

    private static String nombreCompletoEmpleado(Empleado e) {
        PerfilUsuario perfil = e.getUsuario() != null ? e.getUsuario().getPerfil() : null;
        if (perfil == null) return "";
        return perfil.getNombres() + " " + perfil.getApellidos();
    }
}

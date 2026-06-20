package co.edu.sena.productsreact.dto.servicio;

import co.edu.sena.productsreact.entity.SubServicio;

import java.math.BigDecimal;

public record SubServicioResponse(
        Long id,
        Long idServicio,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos,
        Boolean estado
) {
    public static SubServicioResponse from(SubServicio s) {
        return new SubServicioResponse(
                s.getId(),
                s.getServicio().getId(),
                s.getNombre(),
                s.getDescripcion(),
                s.getPrecio(),
                s.getDuracionMinutos(),
                s.getEstado()
        );
    }
}

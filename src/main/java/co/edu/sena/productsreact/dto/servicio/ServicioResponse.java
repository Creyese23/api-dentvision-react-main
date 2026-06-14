package co.edu.sena.productsreact.dto.servicio;

import co.edu.sena.productsreact.entity.Servicio;

import java.math.BigDecimal;

public record ServicioResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos,
        Boolean estado,
        String imagenUrl
) {
    public static ServicioResponse from(Servicio s) {
        return new ServicioResponse(
                s.getId(), s.getNombre(), s.getDescripcion(),
                s.getPrecio(), s.getDuracionMinutos(), s.getEstado(),
                s.getImagenUrl()
        );
    }
}

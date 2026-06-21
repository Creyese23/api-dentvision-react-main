package co.edu.sena.productsreact.dto.servicio;

import co.edu.sena.productsreact.entity.Servicio;

public record ServicioResponse(
        Long id,
        String nombre,
        String descripcion,
        Boolean estado,
        String imagenUrl
) {
    public static ServicioResponse from(Servicio s) {
        return new ServicioResponse(
                s.getId(), s.getNombre(), s.getDescripcion(),
                s.getEstado(), s.getImagenUrl()
        );
    }
}

package co.edu.sena.productsreact.dto.comunicacion;

import co.edu.sena.productsreact.entity.Mensaje;

import java.time.LocalDateTime;

public record MensajeResponse(
        Long id,
        Long idConversacion,
        Long idRemitente,
        String emailRemitente,
        String contenido,
        LocalDateTime hora,
        String estado
) {
    public static MensajeResponse from(Mensaje m) {
        return new MensajeResponse(
                m.getId(), m.getConversacion().getId(),
                m.getRemitente().getId(), m.getRemitente().getEmail(),
                m.getContenido(), m.getHora(), m.getEstado()
        );
    }
}

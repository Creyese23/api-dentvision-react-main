package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.comunicacion.MensajeRequest;
import co.edu.sena.productsreact.dto.comunicacion.MensajeResponse;
import co.edu.sena.productsreact.service.ConversacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversaciones")
@RequiredArgsConstructor
public class ConversacionController {

    private final ConversacionService conversacionService;

    @PostMapping("/iniciar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> iniciar(
            @RequestParam Long pacienteId,
            @RequestParam(required = false) Long procedimientoId) {
        Long id = conversacionService.iniciarConversacion(pacienteId, procedimientoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @GetMapping("/{id}/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MensajeResponse>> mensajes(@PathVariable Long id) {
        return ResponseEntity.ok(conversacionService.mensajes(id));
    }

    @PostMapping("/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MensajeResponse> enviar(@Valid @RequestBody MensajeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversacionService.enviarMensaje(request));
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<Void> cerrar(@PathVariable Long id) {
        conversacionService.cerrarConversacion(id);
        return ResponseEntity.noContent().build();
    }
}

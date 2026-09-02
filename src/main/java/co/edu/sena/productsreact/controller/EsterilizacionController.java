package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.esterilizacion.EsterilizacionRequest;
import co.edu.sena.productsreact.dto.esterilizacion.EsterilizacionResponse;
import co.edu.sena.productsreact.service.EsterilizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/esterilizaciones")
@RequiredArgsConstructor
public class EsterilizacionController {

    private final EsterilizacionService esterilizacionService;

    @GetMapping("/cita/{citaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    public ResponseEntity<EsterilizacionResponse> obtenerPorCita(@PathVariable Long citaId) {
        return ResponseEntity.ok(esterilizacionService.obtenerPorCita(citaId));
    }

    @PutMapping("/cita/{citaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    public ResponseEntity<EsterilizacionResponse> guardarPorCita(
            @PathVariable Long citaId,
            @Valid @RequestBody EsterilizacionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(esterilizacionService.guardarPorCita(citaId, request, authentication.getName()));
    }
}

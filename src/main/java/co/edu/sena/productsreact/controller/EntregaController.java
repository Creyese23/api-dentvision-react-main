package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.entrega.EntregaResponse;
import co.edu.sena.productsreact.service.EntregaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA','ROLE_TECNICO','ROLE_ODONTOLOGO')")
    public ResponseEntity<List<EntregaResponse>> listar() {
        return ResponseEntity.ok(entregaService.listar());
    }

    @GetMapping("/orden/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA','ROLE_TECNICO','ROLE_ODONTOLOGO')")
    public ResponseEntity<EntregaResponse> porOrden(@PathVariable Long id) {
        return entregaService.obtenerPorOrden(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mis-entregas")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<List<EntregaResponse>> misEntregas(Authentication auth) {
        return ResponseEntity.ok(entregaService.listarMisEntregas(auth.getName()));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA','ROLE_TECNICO')")
    public ResponseEntity<EntregaResponse> cambiarEstado(@PathVariable Long id,
                                                          @RequestParam String estado) {
        return ResponseEntity.ok(entregaService.cambiarEstado(id, estado));
    }
}

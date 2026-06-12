package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoRequest;
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoResponse;
import co.edu.sena.productsreact.service.ProcedimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimientos")
@RequiredArgsConstructor
public class ProcedimientoController {

    private final ProcedimientoService procedimientoService;

    @GetMapping("/cita/{citaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<List<ProcedimientoResponse>> porCita(@PathVariable Long citaId) {
        return ResponseEntity.ok(procedimientoService.listarPorCita(citaId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<ProcedimientoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(procedimientoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<ProcedimientoResponse> crear(@Valid @RequestBody ProcedimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procedimientoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<ProcedimientoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProcedimientoRequest request) {
        return ResponseEntity.ok(procedimientoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<ProcedimientoResponse> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(procedimientoService.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        procedimientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

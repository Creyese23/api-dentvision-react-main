package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoInsumoResponse;
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoRequest;
import co.edu.sena.productsreact.dto.procedimiento.ProcedimientoResponse;
import co.edu.sena.productsreact.service.ProcedimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimientos")
@RequiredArgsConstructor
public class ProcedimientoController {

    private final ProcedimientoService procedimientoService;

    @GetMapping("/mi-historial")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<List<ProcedimientoResponse>> miHistorial(Authentication auth) {
        return ResponseEntity.ok(procedimientoService.listarMiHistorial(auth.getName()));
    }

    @GetMapping("/tecnico")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_TECNICO')")
    public ResponseEntity<List<ProcedimientoResponse>> porTecnico(Authentication auth) {
        return ResponseEntity.ok(procedimientoService.listarPorTecnicoAutenticado(auth.getName()));
    }

    @GetMapping("/cita/{citaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    public ResponseEntity<List<ProcedimientoResponse>> porCita(@PathVariable Long citaId) {
        return ResponseEntity.ok(procedimientoService.listarPorCita(citaId));
    }

    /** Historia clinica completa de un paciente (todos sus procedimientos), para uso del odontologo. */
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<List<ProcedimientoResponse>> porPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(procedimientoService.listarPorPaciente(pacienteId));
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_TECNICO')")
    public ResponseEntity<ProcedimientoResponse> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(procedimientoService.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        procedimientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ── Insumos utilizados en el procedimiento ────────────────────────────────

    @GetMapping("/{id}/insumos")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<List<ProcedimientoInsumoResponse>> listarInsumos(@PathVariable Long id) {
        return ResponseEntity.ok(procedimientoService.listarInsumos(id));
    }

    @PostMapping("/{id}/insumos")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<ProcedimientoInsumoResponse> registrarInsumo(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body) {
        Long insumoId = Long.valueOf(body.get("idInsumo").toString());
        Integer cantidad = Integer.valueOf(body.get("cantidad").toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(procedimientoService.registrarInsumo(id, insumoId, cantidad));
    }

    @DeleteMapping("/{id}/insumos/{piId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<Void> eliminarInsumo(@PathVariable Long id, @PathVariable Long piId) {
        procedimientoService.eliminarInsumo(piId);
        return ResponseEntity.noContent().build();
    }
}

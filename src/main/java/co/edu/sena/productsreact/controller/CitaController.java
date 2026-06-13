package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.cita.AgendarCitaRequest;
import co.edu.sena.productsreact.dto.cita.CitaRequest;
import co.edu.sena.productsreact.dto.cita.CitaResponse;
import co.edu.sena.productsreact.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @GetMapping("/mis-citas")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<List<CitaResponse>> misCitas(Authentication auth) {
        return ResponseEntity.ok(citaService.listarMisCitas(auth.getName()));
    }

    @GetMapping("/paciente/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    public ResponseEntity<List<CitaResponse>> porPaciente(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.listarPorPaciente(id));
    }

    @GetMapping("/odontologo/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA')")
    public ResponseEntity<List<CitaResponse>> porOdontologo(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.listarPorOdontologo(id));
    }

    @PostMapping("/agendar")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<CitaResponse> agendar(Authentication auth, @Valid @RequestBody AgendarCitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.agendarCita(auth.getName(), request));
    }

    @GetMapping("/rango")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    public ResponseEntity<List<CitaResponse>> porRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(citaService.listarPorRangoFecha(inicio, fin));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    public ResponseEntity<CitaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    public ResponseEntity<CitaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(citaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA')")
    public ResponseEntity<CitaResponse> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(citaService.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        citaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

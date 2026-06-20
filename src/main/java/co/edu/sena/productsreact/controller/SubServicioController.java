package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.servicio.SubServicioRequest;
import co.edu.sena.productsreact.dto.servicio.SubServicioResponse;
import co.edu.sena.productsreact.service.SubServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios/{servicioId}/sub")
@RequiredArgsConstructor
public class SubServicioController {

    private final SubServicioService subServicioService;

    @GetMapping
    public ResponseEntity<List<SubServicioResponse>> listar(@PathVariable Long servicioId) {
        return ResponseEntity.ok(subServicioService.listarPorServicio(servicioId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SubServicioResponse> crear(
            @PathVariable Long servicioId,
            @Valid @RequestBody SubServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subServicioService.crear(servicioId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SubServicioResponse> actualizar(
            @PathVariable Long servicioId,
            @PathVariable Long id,
            @Valid @RequestBody SubServicioRequest request) {
        return ResponseEntity.ok(subServicioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long servicioId, @PathVariable Long id) {
        subServicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

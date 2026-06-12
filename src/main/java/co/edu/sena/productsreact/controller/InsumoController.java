package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.insumo.InsumoRequest;
import co.edu.sena.productsreact.dto.insumo.InsumoResponse;
import co.edu.sena.productsreact.dto.insumo.MovimientoRequest;
import co.edu.sena.productsreact.service.InsumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insumos")
@RequiredArgsConstructor
public class InsumoController {

    private final InsumoService insumoService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<List<InsumoResponse>> listar() {
        return ResponseEntity.ok(insumoService.listarActivos());
    }

    @GetMapping("/bajo-stock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<List<InsumoResponse>> bajoStock() {
        return ResponseEntity.ok(insumoService.listarBajoStock());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<InsumoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(insumoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<InsumoResponse> crear(@Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(insumoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<InsumoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.ok(insumoService.actualizar(id, request));
    }

    @PostMapping("/movimiento")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO')")
    public ResponseEntity<InsumoResponse> movimiento(@Valid @RequestBody MovimientoRequest request) {
        return ResponseEntity.ok(insumoService.registrarMovimiento(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        insumoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

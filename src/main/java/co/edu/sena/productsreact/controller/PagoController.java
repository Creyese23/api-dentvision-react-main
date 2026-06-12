package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.pago.PagoRequest;
import co.edu.sena.productsreact.dto.pago.PagoResponse;
import co.edu.sena.productsreact.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @GetMapping("/factura/{facturaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    public ResponseEntity<List<PagoResponse>> porFactura(@PathVariable Long facturaId) {
        return ResponseEntity.ok(pagoService.listarPorFactura(facturaId));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    public ResponseEntity<PagoResponse> registrar(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.registrar(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        pagoService.anular(id);
        return ResponseEntity.noContent().build();
    }
}

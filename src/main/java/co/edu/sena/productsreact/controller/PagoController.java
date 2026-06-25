package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.pago.PagoRequest;
import co.edu.sena.productsreact.dto.pago.PagoResponse;
import co.edu.sena.productsreact.service.MercadoPagoService;
import co.edu.sena.productsreact.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final MercadoPagoService mercadoPagoService;

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

    @PostMapping("/mercadopago/preferencia")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<Map<String, String>> crearPreferencia(@RequestBody Map<String, Long> body) {
        Long facturaId = body.get("facturaId");
        if (facturaId == null) return ResponseEntity.badRequest().build();
        String initPoint = mercadoPagoService.crearPreferencia(facturaId);
        return ResponseEntity.ok(Map.of("initPoint", initPoint));
    }

    @PostMapping("/mercadopago/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody(required = false) String payload,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id) {
        mercadoPagoService.procesarWebhook(payload != null ? payload : "", topic, id);
        return ResponseEntity.ok().build();
    }
}

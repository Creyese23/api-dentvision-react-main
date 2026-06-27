package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.pago.PagoRequest;
import co.edu.sena.productsreact.entity.Factura;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.FacturaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class MercadoPagoService {

    private final FacturaRepository facturaRepository;
    private final PagoService pagoService;

    public MercadoPagoService(FacturaRepository facturaRepository, @Lazy PagoService pagoService) {
        this.facturaRepository = facturaRepository;
        this.pagoService = pagoService;
    }

    @Value("${MP_ACCESS_TOKEN:}")
    private String accessToken;

    @Value("${app.frontend-url:https://dentvision-u6z4.onrender.com}")
    private String frontendUrl;

    @Value("${app.backend-url:https://api-dentvision-react-main.onrender.com}")
    private String backendUrl;

    @Transactional(readOnly = true)
    public String crearPreferencia(Long facturaId, String metodoPago) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura " + facturaId + " no encontrada"));

        double total = factura.getTotal().doubleValue();
        String nombrePaciente;
        try {
            nombrePaciente = factura.getPaciente().getUsuario().getPerfil().getNombres()
                    + " " + factura.getPaciente().getUsuario().getPerfil().getApellidos();
        } catch (Exception e) {
            nombrePaciente = "Paciente";
        }

        String body = String.format("""
                {
                  "items": [{
                    "id": "FACTURA-%d",
                    "title": "Factura DentVision #%d",
                    "description": "Consulta odontológica - %s",
                    "quantity": 1,
                    "unit_price": %.2f,
                    "currency_id": "COP"
                  }],
                  "back_urls": {
                    "success": "%s/pago-exitoso?factura=%d&gateway=mp",
                    "failure": "%s/pago-fallido?factura=%d&gateway=mp",
                    "pending": "%s/pago-pendiente?factura=%d&gateway=mp"
                  },
                  "auto_return": "approved",
                  "notification_url": "%s/api/pagos/mercadopago/webhook",
                  "external_reference": "%d",
                  "statement_descriptor": "DENTVISION",
                  %s
                }
                """,
                facturaId, facturaId, nombrePaciente, total,
                frontendUrl, facturaId,
                frontendUrl, facturaId,
                frontendUrl, facturaId,
                backendUrl,
                facturaId,
                buildPaymentMethods(metodoPago));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/checkout/preferences"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("MercadoPago error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Error creando preferencia MercadoPago: " + response.body());
            }

            String respBody = response.body();
            String initPoint = extractJson(respBody, "init_point");
            log.info("Preferencia MP creada para factura {}, metodo {}: {}", facturaId, metodoPago, initPoint);
            return initPoint;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error conectando con MercadoPago: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void procesarWebhook(String payload, String topic, String id) {
        try {
            if (!"payment".equals(topic) && !payload.contains("\"payment\"")) return;

            String paymentId = id != null ? id : extractJson(payload, "id");
            if (paymentId == null || paymentId.isBlank()) return;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentId))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().build();

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            String body = resp.body();
            String status = extractJson(body, "status");
            String extRef = extractJson(body, "external_reference");
            String amountStr = extractJson(body, "transaction_amount");
            String methodId = extractJson(body, "payment_method_id");

            log.info("[MP Webhook] paymentId={} status={} facturaId={} method={}", paymentId, status, extRef, methodId);

            if ("approved".equals(status) && extRef != null) {
                Long facturaId = Long.parseLong(extRef);
                facturaRepository.findById(facturaId).ifPresent(f -> {
                    f.setEstado("PAGADA");
                    facturaRepository.save(f);
                    log.info("[MP Webhook] Factura {} marcada como PAGADA", facturaId);
                });

                try {
                    BigDecimal monto = amountStr != null ? new BigDecimal(amountStr) : BigDecimal.ZERO;
                    String metodo = mapearMetodoPago(methodId);
                    pagoService.registrar(new PagoRequest(facturaId, monto, metodo, null));
                    log.info("[MP Webhook] Pago registrado para factura {} metodo {}", facturaId, metodo);
                } catch (Exception e) {
                    log.warn("[MP Webhook] No se pudo registrar el pago (puede ya existir): {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[MP Webhook] Error procesando notificacion: {}", e.getMessage());
        }
    }

    private String buildPaymentMethods(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) {
            return "\"payment_methods\": { \"installments\": 1 }";
        }
        return switch (metodoPago.toUpperCase()) {
            case "NEQUI" -> """
                "payment_methods": {
                  "excluded_payment_types": [
                    {"id":"credit_card"},{"id":"debit_card"},{"id":"ticket"},
                    {"id":"bank_transfer"},{"id":"atm"},{"id":"prepaid_card"}
                  ],
                  "excluded_payment_methods": [{"id":"daviplata"}],
                  "installments": 1
                }""";
            case "DAVIPLATA" -> """
                "payment_methods": {
                  "excluded_payment_types": [
                    {"id":"credit_card"},{"id":"debit_card"},{"id":"ticket"},
                    {"id":"bank_transfer"},{"id":"atm"},{"id":"prepaid_card"}
                  ],
                  "excluded_payment_methods": [{"id":"nequi"}],
                  "installments": 1
                }""";
            case "TRANSFERENCIA" -> """
                "payment_methods": {
                  "excluded_payment_types": [
                    {"id":"credit_card"},{"id":"debit_card"},{"id":"ticket"},
                    {"id":"digital_currency"},{"id":"atm"},{"id":"prepaid_card"}
                  ],
                  "installments": 1
                }""";
            case "TARJETA" -> """
                "payment_methods": {
                  "excluded_payment_types": [
                    {"id":"ticket"},{"id":"digital_currency"},
                    {"id":"bank_transfer"},{"id":"atm"}
                  ],
                  "installments": 12
                }""";
            default -> "\"payment_methods\": { \"installments\": 1 }";
        };
    }

    private String mapearMetodoPago(String mpMethodId) {
        if (mpMethodId == null) return "MERCADOPAGO";
        return switch (mpMethodId.toLowerCase()) {
            case "nequi"     -> "NEQUI";
            case "daviplata" -> "DAVIPLATA";
            case "pse"       -> "TRANSFERENCIA";
            case "visa", "master", "amex", "diners", "codensa",
                 "debmaster", "debvisa", "electron", "maestro" -> "TARJETA";
            default -> "MERCADOPAGO";
        };
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) {
            String numSearch = "\"" + key + "\":";
            start = json.indexOf(numSearch);
            if (start < 0) return null;
            int s = start + numSearch.length();
            int end = json.indexOf(",", s);
            if (end < 0) end = json.indexOf("}", s);
            return end < 0 ? null : json.substring(s, end).trim().replace("\"", "");
        }
        start += search.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? null : json.substring(start, end);
    }
}

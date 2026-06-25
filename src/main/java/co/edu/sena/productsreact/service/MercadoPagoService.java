package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.entity.Factura;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoService {

    private final FacturaRepository facturaRepository;

    @Value("${MP_ACCESS_TOKEN:}")
    private String accessToken;

    @Value("${app.frontend-url:https://dentvision-u6z4.onrender.com}")
    private String frontendUrl;

    @Value("${app.backend-url:https://api-dentvision-react-main.onrender.com}")
    private String backendUrl;

    @Transactional(readOnly = true)
    public String crearPreferencia(Long facturaId) {
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
                    "success": "%s/pago-exitoso?factura=%d",
                    "failure": "%s/pago-fallido?factura=%d",
                    "pending": "%s/pago-pendiente?factura=%d"
                  },
                  "auto_return": "approved",
                  "notification_url": "%s/api/pagos/mercadopago/webhook",
                  "external_reference": "%d",
                  "statement_descriptor": "DENTVISION",
                  "payment_methods": {
                    "excluded_payment_types": [],
                    "installments": 1
                  }
                }
                """,
                facturaId, facturaId, nombrePaciente, total,
                frontendUrl, facturaId,
                frontendUrl, facturaId,
                frontendUrl, facturaId,
                backendUrl,
                facturaId);

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
            log.info("Preferencia MP creada para factura {}: {}", facturaId, initPoint);
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

            log.info("[MP Webhook] paymentId={} status={} facturaId={}", paymentId, status, extRef);

            if ("approved".equals(status) && extRef != null) {
                Long facturaId = Long.parseLong(extRef);
                facturaRepository.findById(facturaId).ifPresent(f -> {
                    f.setEstado("PAGADA");
                    facturaRepository.save(f);
                    log.info("[MP Webhook] Factura {} marcada como PAGADA", facturaId);
                });
            }
        } catch (Exception e) {
            log.error("[MP Webhook] Error procesando notificación: {}", e.getMessage());
        }
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

package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.pago.PagoResponse;
import co.edu.sena.productsreact.entity.Factura;
import co.edu.sena.productsreact.entity.Pago;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.FacturaRepository;
import co.edu.sena.productsreact.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {

    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;

    @Value("${PAYPAL_CLIENT_ID:}")
    private String clientId;

    @Value("${PAYPAL_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${PAYPAL_MODE:sandbox}")
    private String mode;

    @Value("${PAYPAL_COP_TO_USD_RATE:4200}")
    private double copToUsdRate;

    private String getBaseUrl() {
        return "sandbox".equals(mode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    private String getAccessToken() {
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/v1/oauth2/token"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());
            String token = extractJson(resp.body(), "access_token");
            if (token == null)
                throw new RuntimeException("No se obtuvo access_token de PayPal: " + resp.body());
            return token;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo token PayPal: " + e.getMessage(), e);
        }
    }

    public String crearOrden(Long facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura " + facturaId + " no encontrada"));

        // COP → USD conversion
        BigDecimal usdAmount = factura.getTotal()
                .divide(BigDecimal.valueOf(copToUsdRate), 2, RoundingMode.HALF_UP);
        if (usdAmount.compareTo(BigDecimal.valueOf(0.01)) < 0)
            usdAmount = BigDecimal.valueOf(0.01);

        String token = getAccessToken();
        String body = """
                {
                  "intent": "CAPTURE",
                  "purchase_units": [{
                    "reference_id": "%d",
                    "description": "Factura DentVision #%d",
                    "amount": { "currency_code": "USD", "value": "%s" }
                  }]
                }
                """.formatted(facturaId, facturaId, usdAmount.toPlainString());

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/v2/checkout/orders"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());
            log.info("[PayPal] crear-orden factura={} status={}", facturaId, resp.statusCode());

            if (resp.statusCode() >= 400)
                throw new RuntimeException("PayPal error " + resp.statusCode() + ": " + resp.body());

            String orderId = extractJson(resp.body(), "id");
            if (orderId == null)
                throw new RuntimeException("PayPal no devolvió orderId: " + resp.body());

            log.info("[PayPal] Orden creada: {} para factura {}", orderId, facturaId);
            return orderId;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error conectando con PayPal: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PagoResponse capturarOrden(String orderId, Long facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura " + facturaId + " no encontrada"));

        String token = getAccessToken();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());
            log.info("[PayPal] captura orden={} status={}", orderId, resp.statusCode());

            if (resp.statusCode() >= 400)
                throw new RuntimeException("Error capturando PayPal " + resp.statusCode() + ": " + resp.body());

            String status = extractJson(resp.body(), "status");
            if (!"COMPLETED".equalsIgnoreCase(status))
                throw new RuntimeException("Pago PayPal no completado. Estado: " + status);

            // Registrar saldo restante como pago
            BigDecimal totalPagado = pagoRepository.sumValorByFacturaId(facturaId);
            BigDecimal saldo = factura.getTotal()
                    .subtract(totalPagado != null ? totalPagado : BigDecimal.ZERO);

            Pago pago = Pago.builder()
                    .factura(factura)
                    .valor(saldo.max(BigDecimal.ZERO))
                    .metodoPago("PAYPAL")
                    .estado("COMPLETADO")
                    .fechaPago(LocalDateTime.now())
                    .build();
            Pago saved = pagoRepository.save(pago);

            BigDecimal nuevoTotal = pagoRepository.sumValorByFacturaId(facturaId);
            if (nuevoTotal != null && nuevoTotal.compareTo(factura.getTotal()) >= 0) {
                factura.setEstado("PAGADA");
                facturaRepository.save(factura);
            }

            log.info("[PayPal] Pago id={} registrado para factura={}", saved.getId(), facturaId);
            return PagoResponse.from(saved);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error capturando pago PayPal: " + e.getMessage(), e);
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

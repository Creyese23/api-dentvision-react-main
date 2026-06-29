package co.edu.sena.productsreact.service;

// Importación del DTO de pago para registrar el pago cuando MercadoPago confirma la transacción
import co.edu.sena.productsreact.dto.pago.PagoRequest;

// Entidad de la factura que se vincula al pago de MercadoPago
import co.edu.sena.productsreact.entity.Factura;

// Excepción cuando no se encuentra la factura solicitada
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Repositorio para buscar y actualizar facturas en la BD
import co.edu.sena.productsreact.repository.FacturaRepository;

import lombok.extern.slf4j.Slf4j;                       // Genera el objeto "log" para registrar eventos de la integración
import org.springframework.beans.factory.annotation.Value; // Para leer propiedades del archivo application.properties
import org.springframework.context.annotation.Lazy;         // Para romper dependencias circulares (MercadoPagoService usa PagoService)
import org.springframework.stereotype.Service;             // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones al procesar webhooks

import java.math.BigDecimal; // Para manejar el monto del pago con precisión exacta
import java.net.URI;         // Para construir la URL de la API de MercadoPago
import java.net.http.HttpClient;   // Cliente HTTP nativo de Java 11+ para hacer peticiones REST
import java.net.http.HttpRequest;  // Para construir las peticiones HTTP (headers, body, método)
import java.net.http.HttpResponse; // Para recibir y procesar las respuestas HTTP de MercadoPago
import java.time.Duration;         // Para definir el tiempo máximo de espera de las peticiones HTTP

// @Service: Spring registra esta clase como componente de integración con MercadoPago
@Service
// @Slf4j: Lombok genera "log" para registrar eventos importantes de la integración (creación de preferencias, webhooks)
@Slf4j
public class MercadoPagoService {

    // Repositorio para buscar la factura y actualizar su estado a PAGADA cuando se recibe confirmación
    private final FacturaRepository facturaRepository;
    // Servicio de pagos para registrar el pago en la BD cuando MercadoPago confirma la transacción
    // @Lazy evita dependencia circular: PagoService no depende de MercadoPagoService, pero se inyecta de forma diferida
    private final PagoService pagoService;

    // Constructor manual (no usa @RequiredArgsConstructor porque PagoService necesita @Lazy)
    public MercadoPagoService(FacturaRepository facturaRepository, @Lazy PagoService pagoService) {
        this.facturaRepository = facturaRepository;
        this.pagoService = pagoService;
    }

    // @Value: inyecta el token de acceso de MercadoPago desde variables de entorno o application.properties
    // Si no está configurado, usa cadena vacía (la API fallará pero no rompe el inicio de la aplicación)
    @Value("${MP_ACCESS_TOKEN:}")
    private String accessToken;

    // URL del frontend, usada para construir las URLs de retorno después del pago (success, failure, pending)
    @Value("${app.frontend-url:https://dentvision-u6z4.onrender.com}")
    private String frontendUrl;

    // URL del backend, usada para configurar la URL del webhook de notificaciones de MercadoPago
    @Value("${app.backend-url:https://api-dentvision-react-main.onrender.com}")
    private String backendUrl;

    /**
     * Crea una preferencia de pago en MercadoPago para una factura específica.
     * Retorna la URL (init_point) a la que se redirige al usuario para completar el pago.
     * El método de pago determina qué opciones de pago se muestran en MercadoPago.
     */
    @Transactional(readOnly = true) // Solo lectura de la factura; no modifica la BD
    public String crearPreferencia(Long facturaId, String metodoPago) {
        // Busca la factura en la BD para obtener el monto y datos del paciente
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura " + facturaId + " no encontrada"));

        // Obtiene el monto total de la factura como número decimal para incluir en el JSON
        double total = factura.getTotal().doubleValue();
        // Intenta obtener el nombre completo del paciente para la descripción del pago
        String nombrePaciente;
        try {
            // Navega por la cadena: factura → paciente → usuario → perfil → nombres + apellidos
            nombrePaciente = factura.getPaciente().getUsuario().getPerfil().getNombres()
                    + " " + factura.getPaciente().getUsuario().getPerfil().getApellidos();
        } catch (Exception e) {
            nombrePaciente = "Paciente"; // Si falla, usa nombre genérico para no bloquear el pago
        }

        // Construye el JSON del body de la preferencia de pago usando text blocks de Java 15+
        // Este JSON le dice a MercadoPago qué cobrar, cuánto, y a dónde redirigir al usuario
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
                frontendUrl, facturaId,   // URL de éxito: a dónde va el usuario si el pago fue exitoso
                frontendUrl, facturaId,   // URL de fallo: a dónde va el usuario si el pago falla
                frontendUrl, facturaId,   // URL de pendiente: a dónde va si el pago queda en revisión
                backendUrl,               // URL del webhook: MercadoPago enviará notificaciones aquí
                facturaId,                // external_reference: ID de la factura para identificar el pago en el webhook
                buildPaymentMethods(metodoPago)); // Configuración de métodos de pago según el método elegido

        try {
            // Construye la petición HTTP POST a la API de MercadoPago para crear la preferencia
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/checkout/preferences")) // Endpoint de MercadoPago
                    .timeout(Duration.ofSeconds(15))          // Tiempo máximo de espera: 15 segundos
                    .header("Authorization", "Bearer " + accessToken) // Token de acceso como Bearer token
                    .header("Content-Type", "application/json")       // El body es JSON
                    .POST(HttpRequest.BodyPublishers.ofString(body))  // Envía el JSON como cuerpo de la petición
                    .build();

            // Envía la petición y espera la respuesta de MercadoPago
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            // Si MercadoPago devuelve un error (código 400+), lanza excepción con el mensaje de error
            if (response.statusCode() >= 400) {
                log.error("MercadoPago error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Error creando preferencia MercadoPago: " + response.body());
            }

            String respBody = response.body(); // Cuerpo de la respuesta JSON de MercadoPago
            // Extrae el campo "init_point" del JSON: es la URL a la que se redirige al usuario para pagar
            String initPoint = extractJson(respBody, "init_point");
            log.info("Preferencia MP creada para factura {}, metodo {}: {}", facturaId, metodoPago, initPoint);
            return initPoint; // Retorna la URL de pago de MercadoPago

        } catch (RuntimeException e) {
            throw e; // Re-lanza excepciones de negocio sin envolverlas
        } catch (Exception e) {
            // Envuelve excepciones de red o IO en RuntimeException para que Spring las maneje
            throw new RuntimeException("Error conectando con MercadoPago: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa las notificaciones (webhooks) que MercadoPago envía cuando un pago cambia de estado.
     * Si el pago está "approved", actualiza la factura como PAGADA y registra el pago en la BD.
     * Este método es invocado por el controlador cuando MercadoPago hace una petición POST al webhook.
     */
    @Transactional // Esta operación puede actualizar la factura y crear un pago en la BD
    public void procesarWebhook(String payload, String topic, String id) {
        try {
            // Solo procesa notificaciones de tipo "payment" (ignora otros tipos como merchant_orders)
            if (!"payment".equals(topic) && !payload.contains("\"payment\"")) return;

            // Obtiene el ID del pago de MercadoPago: puede venir como parámetro URL o en el payload JSON
            String paymentId = id != null ? id : extractJson(payload, "id");
            if (paymentId == null || paymentId.isBlank()) return; // Si no hay ID de pago, ignora la notificación

            // Consulta los detalles del pago a la API de MercadoPago para verificar el estado real
            // (no se confía en los datos del webhook para seguridad; siempre se consulta a MercadoPago)
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/v1/payments/" + paymentId)) // Endpoint de detalle de pago
                    .timeout(Duration.ofSeconds(10))          // Tiempo máximo de espera: 10 segundos
                    .header("Authorization", "Bearer " + accessToken) // Token de acceso
                    .GET().build(); // Es una consulta GET (solo lectura de MercadoPago)

            // Envía la petición y obtiene los detalles del pago
            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            String body = resp.body(); // Cuerpo JSON con los detalles del pago
            String status = extractJson(body, "status");              // Estado del pago en MercadoPago (approved, pending, rejected)
            String extRef = extractJson(body, "external_reference");  // ID de la factura (enviado cuando se creó la preferencia)
            String amountStr = extractJson(body, "transaction_amount"); // Monto cobrado por MercadoPago
            String methodId = extractJson(body, "payment_method_id");   // Método de pago usado (nequi, visa, pse, etc.)

            log.info("[MP Webhook] paymentId={} status={} facturaId={} method={}", paymentId, status, extRef, methodId);

            // Solo procesa pagos aprobados; ignora los pendientes o rechazados
            if ("approved".equals(status) && extRef != null) {
                Long facturaId = Long.parseLong(extRef); // Convierte el ID de factura de String a Long
                // Actualiza la factura a PAGADA en la BD si existe
                facturaRepository.findById(facturaId).ifPresent(f -> {
                    f.setEstado("PAGADA");               // Marca la factura como pagada
                    facturaRepository.save(f);           // Persiste el cambio
                    log.info("[MP Webhook] Factura {} marcada como PAGADA", facturaId);
                });

                // Registra el pago en la BD del sistema para trazabilidad contable
                try {
                    // Convierte el monto de String a BigDecimal; si no viene, usa cero
                    BigDecimal monto = amountStr != null ? new BigDecimal(amountStr) : BigDecimal.ZERO;
                    // Mapea el ID del método de pago de MercadoPago a un nombre legible del sistema
                    String metodo = mapearMetodoPago(methodId);
                    // Registra el pago usando PagoService para mantener la consistencia de datos
                    pagoService.registrar(new PagoRequest(facturaId, monto, metodo, null));
                    log.info("[MP Webhook] Pago registrado para factura {} metodo {}", facturaId, metodo);
                } catch (Exception e) {
                    // Si el pago ya fue registrado antes (doble notificación), solo registra advertencia
                    log.warn("[MP Webhook] No se pudo registrar el pago (puede ya existir): {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            // Si falla el procesamiento del webhook, registra el error pero no lanza excepción
            // (MercadoPago reintentará la notificación si recibe error HTTP)
            log.error("[MP Webhook] Error procesando notificacion: {}", e.getMessage());
        }
    }

    /**
     * Construye la sección JSON de configuración de métodos de pago según el método elegido por el usuario.
     * Cada método excluye los tipos de pago que no corresponden para guiar al usuario al método correcto.
     */
    private String buildPaymentMethods(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) {
            // Si no se especifica método, permite todos con máximo 1 cuota
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
                }"""; // Solo Nequi: excluye tarjetas, tickets, transferencias y Daviplata
            case "DAVIPLATA" -> """
                "payment_methods": {
                  "excluded_payment_types": [
                    {"id":"credit_card"},{"id":"debit_card"},{"id":"ticket"},
                    {"id":"bank_transfer"},{"id":"atm"},{"id":"prepaid_card"}
                  ],
                  "excluded_payment_methods": [{"id":"nequi"}],
                  "installments": 1
                }"""; // Solo Daviplata: excluye tarjetas, tickets, transferencias y Nequi
            case "TRANSFERENCIA" -> """
                "payment_methods": {
                  "excluded_payment_types": [
                    {"id":"credit_card"},{"id":"debit_card"},{"id":"ticket"},
                    {"id":"digital_currency"},{"id":"atm"},{"id":"prepaid_card"}
                  ],
                  "installments": 1
                }"""; // Solo PSE (transferencia bancaria): excluye tarjetas y billeteras digitales
            case "TARJETA" -> """
                "payment_methods": {
                  "excluded_payment_types": [
                    {"id":"ticket"},{"id":"digital_currency"},
                    {"id":"bank_transfer"},{"id":"atm"}
                  ],
                  "installments": 12
                }"""; // Solo tarjetas: excluye tickets, billeteras y transferencias; permite hasta 12 cuotas
            default -> "\"payment_methods\": { \"installments\": 1 }"; // Método desconocido: permite todos con 1 cuota
        };
    }

    /**
     * Mapea el ID del método de pago de MercadoPago a un nombre legible del sistema DentVision.
     * Se usa al registrar el pago en la BD después de procesar el webhook.
     */
    private String mapearMetodoPago(String mpMethodId) {
        if (mpMethodId == null) return "MERCADOPAGO"; // Si no hay método, usa el nombre genérico
        return switch (mpMethodId.toLowerCase()) {
            case "nequi"     -> "NEQUI";         // Pago con Nequi (billetera digital colombiana)
            case "daviplata" -> "DAVIPLATA";     // Pago con Daviplata (billetera de Davivienda)
            case "pse"       -> "TRANSFERENCIA"; // Pago con PSE (transferencia bancaria en línea)
            // Marcas de tarjetas de crédito y débito que MercadoPago puede identificar
            case "visa", "master", "amex", "diners", "codensa",
                 "debmaster", "debvisa", "electron", "maestro" -> "TARJETA";
            default -> "MERCADOPAGO"; // Cualquier otro método se registra como MERCADOPAGO genérico
        };
    }

    /**
     * Extrae el valor de un campo del JSON de respuesta de MercadoPago de forma manual (sin librería).
     * Maneja tanto valores de tipo String (con comillas) como numéricos (sin comillas).
     * Retorna null si el campo no existe en el JSON.
     */
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\""; // Patrón para valores String: "key":"value"
        int start = json.indexOf(search);
        if (start < 0) {
            // El campo no tiene valor String; intenta buscarlo como valor numérico: "key":value
            String numSearch = "\"" + key + "\":";
            start = json.indexOf(numSearch);
            if (start < 0) return null; // El campo no existe en el JSON
            int s = start + numSearch.length(); // Posición donde empieza el valor
            int end = json.indexOf(",", s);      // Busca el siguiente separador de campo
            if (end < 0) end = json.indexOf("}", s); // O el cierre del objeto JSON
            return end < 0 ? null : json.substring(s, end).trim().replace("\"", ""); // Retorna el valor limpio
        }
        start += search.length(); // Avanza al inicio del valor String
        int end = json.indexOf("\"", start); // Busca la comilla de cierre del valor
        return end < 0 ? null : json.substring(start, end); // Retorna el valor entre comillas
    }
}

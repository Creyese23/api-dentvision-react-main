package co.edu.sena.productsreact.service;

// Importaciones de DTOs para pagos
import co.edu.sena.productsreact.dto.pago.PagoRequest;  // DTO con los datos del pago (facturaId, valor, método, fecha)
import co.edu.sena.productsreact.dto.pago.PagoResponse; // DTO con los datos del pago que se devuelven al cliente

// Entidades JPA que representan tablas en la BD
import co.edu.sena.productsreact.entity.Factura; // Entidad de la factura a la que se aplica el pago
import co.edu.sena.productsreact.entity.Pago;    // Entidad que representa un pago realizado por el paciente

// Excepción cuando no se encuentra el recurso solicitado
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Repositorios para acceder a las tablas en la BD
import co.edu.sena.productsreact.repository.FacturaRepository; // Repositorio para buscar y actualizar facturas
import co.edu.sena.productsreact.repository.NotificacionRepository; // Repositorio para guardar notificaciones in-app
import co.edu.sena.productsreact.repository.PagoRepository;    // Repositorio para registrar y consultar pagos

import co.edu.sena.productsreact.entity.Notificacion;

import lombok.RequiredArgsConstructor;        // Genera el constructor con inyección de dependencias
import lombok.extern.slf4j.Slf4j;            // Genera el objeto "log" para registrar advertencias
import org.springframework.stereotype.Service; // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para manejar transacciones de escritura

import java.math.BigDecimal;    // Para manejar valores monetarios con precisión exacta
import java.time.LocalDateTime; // Para registrar la fecha y hora del pago
import java.util.List;          // Colección de elementos

// @Service: Spring registra esta clase como componente de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok inyecta los dos repositorios y el servicio de email por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): las consultas son de solo lectura por defecto
@org.springframework.transaction.annotation.Transactional(readOnly = true)
// @Slf4j: Lombok genera "log" para advertencias cuando el email falla
@Slf4j
public class PagoService {

    // Repositorio para registrar nuevos pagos y consultar el historial de pagos
    private final PagoRepository pagoRepository;
    // Repositorio para buscar la factura y actualizarla a PAGADA cuando se complete el pago
    private final FacturaRepository facturaRepository;
    // Servicio para enviar el email de confirmación de pago al paciente
    private final EmailService emailService;
    // Repositorio para guardar notificaciones in-app
    private final NotificacionRepository notificacionRepository;

    /**
     * Lista todos los pagos registrados para una factura específica.
     * Una factura puede tener múltiples pagos parciales (abonos) hasta completar el total.
     */
    public List<PagoResponse> listarPorFactura(Long facturaId) {
        // Busca pagos de la factura que no tienen fecha de eliminación (no anulados)
        return pagoRepository.findByFacturaIdAndFechaEliminacionIsNull(facturaId)
                .stream().map(PagoResponse::from).toList();
    }

    @Transactional // Esta operación puede crear un pago y actualizar el estado de la factura
    /**
     * Registra un nuevo pago para una factura.
     * Después del pago, calcula el total pagado acumulado; si alcanza o supera el total de la factura,
     * la marca automáticamente como PAGADA.
     * Envía email de confirmación al paciente.
     */
    public PagoResponse registrar(PagoRequest req) {
        // Busca la factura a la que se aplica el pago; lanza excepción si no existe
        Factura factura = facturaRepository.findById(req.idFactura())
                .orElseThrow(() -> new ResourceNotFoundException("Factura " + req.idFactura() + " no encontrada"));

        // Construye la entidad Pago con los datos del request
        Pago pago = Pago.builder()
                .factura(factura)              // Factura a la que pertenece este pago
                .valor(req.valor())            // Valor del pago (puede ser el total o un abono parcial)
                .metodoPago(req.metodoPago())  // Método de pago: EFECTIVO, TARJETA, NEQUI, PAYPAL, etc.
                .estado("COMPLETADO")          // El pago se registra directamente como COMPLETADO
                // Si no se envió fecha de pago, usa la fecha y hora actual
                .fechaPago(req.fechaPago() != null ? req.fechaPago() : LocalDateTime.now())
                .build();
        Pago saved = pagoRepository.save(pago); // Persiste el pago en la BD

        // Calcula el total acumulado de todos los pagos no anulados de esta factura
        BigDecimal totalPagado = pagoRepository.sumValorByFacturaId(factura.getId());
        // Verifica si el total pagado es igual o mayor al total de la factura (puede haber más de un abono)
        boolean facturaCompleta = totalPagado.compareTo(factura.getTotal()) >= 0;
        if (facturaCompleta) {
            // Si ya se pagó todo, cambia el estado de la factura a PAGADA automáticamente
            factura.setEstado("PAGADA");
            facturaRepository.save(factura); // Persiste el cambio de estado en la BD
        }

        // Intenta enviar email de confirmación de pago al paciente
        try {
            String email = factura.getPaciente().getUsuario().getEmail();           // Email del paciente
            String nombre = factura.getPaciente().getUsuario().getPerfil().getNombres(); // Nombre del paciente
            // Formatea el valor del pago con separadores de miles (ej. "150.000")
            String valor = String.format("%,.0f", req.valor()).replace(",", ".");
            // Envía el email con los detalles del pago y si la factura quedó totalmente pagada
            emailService.sendPagoConfirmado(email, nombre, factura.getId(), valor, req.metodoPago(), facturaCompleta);
        } catch (Exception e) {
            // Si el email falla, no interrumpe el registro del pago; solo registra advertencia
            log.warn("No se pudo enviar email de pago: {}", e.getMessage());
        }
        // Notificación in-app al paciente
        try {
            co.edu.sena.productsreact.entity.User userPaciente = factura.getPaciente() != null
                    ? factura.getPaciente().getUsuario() : null;
            if (userPaciente != null) {
                String montoFmt = String.format("%,.0f", req.valor()).replace(",", ".");
                notificacionRepository.save(Notificacion.builder()
                        .usuario(userPaciente)
                        .tipo("PAGO")
                        .mensaje("Pago de $" + montoFmt + " registrado exitosamente")
                        .build());
            }
        } catch (Exception e) {
            log.warn("No se pudo guardar notificación de pago: {}", e.getMessage());
        }

        // Retorna el DTO del pago registrado
        return PagoResponse.from(saved);
    }

    @Transactional // Esta operación anula un pago en la BD
    /**
     * Anula un pago existente marcándolo como ANULADO y registrando la fecha de anulación.
     * El pago anulado ya no se considera en el total pagado de la factura.
     */
    public void anular(Long id) {
        // Busca el pago por ID; lanza excepción con el ID si no se encuentra
        Pago p = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago " + id + " no encontrado"));
        p.setEstado("ANULADO");                         // Marca el pago como anulado (no se puede volver a activar)
        p.setFechaEliminacion(LocalDateTime.now());     // Registra la fecha y hora en que se anuló el pago
        pagoRepository.save(p);                         // Persiste el cambio en la BD
    }
}

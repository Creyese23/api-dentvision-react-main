package co.edu.sena.productsreact.controller;

// Importa los DTOs relacionados con la facturación
import co.edu.sena.productsreact.dto.factura.DetalleFacturaRequest; // Datos de una línea de detalle a agregar a una factura existente
import co.edu.sena.productsreact.dto.factura.FacturaRequest;        // Datos para crear una nueva factura
import co.edu.sena.productsreact.dto.factura.FacturaResponse;       // Datos de la factura que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.FacturaService;            // Servicio con la lógica de negocio de facturación
import jakarta.validation.Valid;                                     // Activa las validaciones declaradas en los campos del DTO
import lombok.RequiredArgsConstructor;                               // Genera el constructor para inyección de dependencias
import org.springframework.http.HttpStatus;                          // Enum con códigos de estado HTTP
import org.springframework.http.ResponseEntity;                      // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize;    // Restringe acceso por rol antes de ejecutar el método
import org.springframework.web.bind.annotation.*;                    // Anotaciones REST del controlador

import java.util.List; // Permite devolver listas de facturas

// @RestController marca esta clase como controlador REST (serializa respuestas a JSON automáticamente)
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/facturas
@RequestMapping("/api/facturas")
// @RequiredArgsConstructor genera el constructor que inyecta facturaService
@RequiredArgsConstructor
public class FacturaController {

    // Servicio que encapsula toda la lógica de negocio de facturación odontológica
    private final FacturaService facturaService;

    // @GetMapping("/paciente/{id}") mapea GET /api/facturas/paciente/{id} (historial de facturas de un paciente)
    @GetMapping("/paciente/{id}")
    // Personal clínico, administrativo y el propio paciente pueden ver las facturas de un paciente
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    // @PathVariable Long id: extrae el ID del paciente de la URL para consultar sus facturas
    public ResponseEntity<List<FacturaResponse>> porPaciente(@PathVariable Long id) {
        // Delega al servicio la consulta de todas las facturas emitidas al paciente con ese ID
        // ResponseEntity.ok() responde 200 OK con la lista de FacturaResponse del paciente
        return ResponseEntity.ok(facturaService.listarPorPaciente(id));
    }

    // @GetMapping("/{id}") mapea GET /api/facturas/{id} (detalle de una factura específica)
    @GetMapping("/{id}")
    // ADMIN, RECEPCIONISTA y el PACIENTE pueden ver el detalle de una factura específica
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    // @PathVariable Long id: extrae el ID de la factura de la URL
    public ResponseEntity<FacturaResponse> obtener(@PathVariable Long id) {
        // Delega al servicio la búsqueda de la factura con ese ID; lanza excepción si no existe
        // ResponseEntity.ok() responde 200 OK con los datos completos de la factura (encabezado + detalles)
        return ResponseEntity.ok(facturaService.obtener(id));
    }

    // @PostMapping mapea POST /api/facturas (crea una nueva factura manualmente)
    @PostMapping
    // Solo ADMIN o RECEPCIONISTA pueden emitir facturas manualmente
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @Valid valida los campos del DTO; @RequestBody deserializa el JSON en un objeto FacturaRequest
    public ResponseEntity<FacturaResponse> crear(@Valid @RequestBody FacturaRequest request) {
        // Delega al servicio la creación de la factura con los datos del request (paciente, fecha, concepto, etc.)
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso (factura)
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.crear(request));
    }

    // @PostMapping("/{id}/detalle") mapea POST /api/facturas/{id}/detalle (agrega una línea de detalle a la factura)
    @PostMapping("/{id}/detalle")
    // Solo ADMIN o RECEPCIONISTA pueden agregar líneas de detalle a una factura existente
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @PathVariable Long id: ID de la factura a la que se agregará el detalle
    // @Valid + @RequestBody DetalleFacturaRequest: valida y deserializa el JSON con el concepto y precio del detalle
    public ResponseEntity<FacturaResponse> agregarDetalle(@PathVariable Long id, @Valid @RequestBody DetalleFacturaRequest request) {
        // Delega al servicio la adición de la línea de detalle a la factura con ese ID
        // ResponseEntity.ok() responde 200 OK con la FacturaResponse actualizada incluyendo el nuevo detalle
        return ResponseEntity.ok(facturaService.agregarDetalle(id, request));
    }

    // @PatchMapping("/{id}/estado") mapea PATCH /api/facturas/{id}/estado?estado=PAGADA
    // PATCH se usa para actualización parcial (solo se cambia el estado de pago de la factura)
    @PatchMapping("/{id}/estado")
    // Solo ADMIN o RECEPCIONISTA pueden cambiar el estado de pago de una factura
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @PathVariable Long id: ID de la factura cuyo estado de pago se quiere cambiar
    // @RequestParam String estado: nuevo estado de la factura (ej. PENDIENTE, PAGADA, ANULADA)
    public ResponseEntity<FacturaResponse> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        // Delega al servicio el cambio de estado de la factura; el servicio puede registrar el pago automáticamente
        // ResponseEntity.ok() responde 200 OK con la FacturaResponse actualizada con el nuevo estado de pago
        return ResponseEntity.ok(facturaService.cambiarEstado(id, estado));
    }

    // @PostMapping("/desde-cita/{citaId}") mapea POST /api/facturas/desde-cita/{citaId}
    // (genera automáticamente una factura a partir de una cita completada)
    @PostMapping("/desde-cita/{citaId}")
    // Solo ADMIN o RECEPCIONISTA pueden generar facturas desde citas
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @PathVariable Long citaId: ID de la cita a partir de la cual se generará la factura automáticamente
    public ResponseEntity<FacturaResponse> crearDesdeCita(@PathVariable Long citaId) {
        // Delega al servicio la generación de la factura usando los datos de la cita (paciente, servicios, odontólogo, etc.)
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso (factura)
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.crearDesdeCita(citaId));
    }

    // @DeleteMapping("/{id}") mapea DELETE /api/facturas/{id} (elimina una factura del sistema)
    @DeleteMapping("/{id}")
    // Solo el ADMIN puede eliminar facturas (acción destructiva e irreversible)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // @PathVariable Long id: ID de la factura a eliminar
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        // Delega al servicio la eliminación de la factura con ese ID del sistema
        facturaService.eliminar(id);
        // ResponseEntity.noContent().build() responde 204 NO CONTENT: operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}

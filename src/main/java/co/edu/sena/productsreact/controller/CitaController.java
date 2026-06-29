package co.edu.sena.productsreact.controller;

// Importa los DTOs usados para agendar y responder citas
import co.edu.sena.productsreact.dto.cita.AgendarCitaRequest; // Datos que envía el paciente para agendar o reagendar una cita
import co.edu.sena.productsreact.dto.cita.CitaRequest;        // Datos que envía el personal administrativo para crear/actualizar citas
import co.edu.sena.productsreact.dto.cita.CitaResponse;       // Datos de la cita que el servidor devuelve al cliente
import co.edu.sena.productsreact.service.CitaService;         // Servicio con la lógica de negocio de citas
import jakarta.validation.Valid;                              // Activa las validaciones declaradas en los DTOs
import lombok.RequiredArgsConstructor;                        // Genera el constructor para inyección de dependencias
import org.springframework.format.annotation.DateTimeFormat;  // Permite especificar el formato esperado de fechas en parámetros de URL
import org.springframework.http.HttpStatus;                   // Enum con códigos de estado HTTP
import org.springframework.http.ResponseEntity;               // Envuelve la respuesta HTTP con estado y cuerpo
import org.springframework.security.access.prepost.PreAuthorize; // Define reglas de acceso por rol antes de ejecutar el método
import org.springframework.security.core.Authentication;      // Contiene la identidad del usuario autenticado
import org.springframework.web.bind.annotation.*;             // Anotaciones REST: @GetMapping, @PostMapping, @PutMapping, etc.

import java.time.LocalDate;     // Representa una fecha sin hora (ej. 2024-06-15)
import java.time.LocalDateTime; // Representa una fecha con hora (ej. 2024-06-15T10:30:00)
import java.util.List;          // Permite devolver listas de citas

// @RestController marca esta clase como controlador REST que serializa respuestas a JSON automáticamente
@RestController
// @RequestMapping define que todos los endpoints de esta clase tienen el prefijo /api/citas
@RequestMapping("/api/citas")
// @RequiredArgsConstructor genera el constructor que inyecta citaService
@RequiredArgsConstructor
public class CitaController {

    // Servicio que contiene toda la lógica de negocio relacionada con citas médicas odontológicas
    private final CitaService citaService;

    // @GetMapping("/mis-citas") mapea GET /api/citas/mis-citas a este método
    @GetMapping("/mis-citas")
    // Solo un paciente autenticado puede ver sus propias citas
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    // Authentication auth: Spring inyecta la información del usuario autenticado desde el token JWT
    public ResponseEntity<List<CitaResponse>> misCitas(Authentication auth) {
        // auth.getName() retorna el email del paciente autenticado
        // Delega al servicio la consulta de todas las citas del paciente identificado por ese email
        // ResponseEntity.ok() responde 200 OK con la lista de citas del paciente
        return ResponseEntity.ok(citaService.listarMisCitas(auth.getName()));
    }

    /** Horarios disponibles para un odontólogo en una fecha (accesible al paciente) */
    // @GetMapping("/disponibles") mapea GET /api/citas/disponibles?odontologoId=1&fecha=2024-06-15
    @GetMapping("/disponibles")
    // Paciente, admin y recepcionista pueden consultar horarios disponibles
    @PreAuthorize("hasAnyAuthority('ROLE_PACIENTE','ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @RequestParam Long odontologoId: extrae el ID del odontólogo de la URL para filtrar su agenda
    // @RequestParam @DateTimeFormat: extrae la fecha de la URL en formato ISO (yyyy-MM-dd) y la convierte a LocalDate
    public ResponseEntity<List<String>> disponibles(
            @RequestParam Long odontologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        // Delega al servicio el cálculo de horas disponibles para el odontólogo en esa fecha
        // ResponseEntity.ok() responde 200 OK con una lista de horas disponibles en formato "HH:mm"
        return ResponseEntity.ok(citaService.horasDisponibles(odontologoId, fecha));
    }

    /** Paciente cancela su propia cita */
    // @PatchMapping("/{id}/cancelar-paciente") mapea PATCH /api/citas/{id}/cancelar-paciente
    // PATCH se usa para actualización parcial (solo cambia el estado de la cita a CANCELADA)
    @PatchMapping("/{id}/cancelar-paciente")
    // Solo el propio paciente puede cancelar sus citas a través de este endpoint
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    // @PathVariable Long id: extrae el ID de la cita a cancelar desde la URL
    // Authentication auth: identidad del paciente autenticado para verificar que la cita le pertenece
    public ResponseEntity<CitaResponse> cancelarPaciente(@PathVariable Long id, Authentication auth) {
        // Delega al servicio la cancelación de la cita; el servicio verifica que el paciente sea el dueño de la cita
        // ResponseEntity.ok() responde 200 OK con los datos actualizados de la cita (estado CANCELADA)
        return ResponseEntity.ok(citaService.cancelarPorPaciente(id, auth.getName()));
    }

    /** Paciente reagenda su propia cita */
    // @PutMapping("/{id}/reagendar") mapea PUT /api/citas/{id}/reagendar
    @PutMapping("/{id}/reagendar")
    // Solo el propio paciente puede reagendar su cita a través de este endpoint
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    // @PathVariable Long id: ID de la cita a reagendar
    // Authentication auth: identidad del paciente para verificar propiedad de la cita
    // @Valid + @RequestBody AgendarCitaRequest: valida y deserializa el JSON con la nueva fecha/hora
    public ResponseEntity<CitaResponse> reagendar(
            @PathVariable Long id,
            Authentication auth,
            @Valid @RequestBody AgendarCitaRequest req) {
        // Delega al servicio el reagendamiento; verifica disponibilidad del odontólogo en la nueva fecha
        // ResponseEntity.ok() responde 200 OK con los datos de la cita con la nueva fecha asignada
        return ResponseEntity.ok(citaService.reagendarPorPaciente(id, auth.getName(), req));
    }

    // @GetMapping("/paciente/{id}") mapea GET /api/citas/paciente/{id} (citas de un paciente específico)
    @GetMapping("/paciente/{id}")
    // Personal clínico y administrativo más el propio paciente pueden ver las citas de un paciente
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    // @PathVariable Long id: ID del paciente del que se quieren ver las citas
    public ResponseEntity<List<CitaResponse>> porPaciente(@PathVariable Long id) {
        // Delega al servicio la consulta de todas las citas asociadas al paciente con ese ID
        // ResponseEntity.ok() responde 200 OK con la lista de CitaResponse del paciente
        return ResponseEntity.ok(citaService.listarPorPaciente(id));
    }

    // @GetMapping("/odontologo/{id}") mapea GET /api/citas/odontologo/{id} (agenda de un odontólogo)
    @GetMapping("/odontologo/{id}")
    // Solo el personal clínico y administrativo puede ver la agenda de un odontólogo
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA')")
    // @PathVariable Long id: ID del odontólogo cuya agenda se quiere consultar
    public ResponseEntity<List<CitaResponse>> porOdontologo(@PathVariable Long id) {
        // Delega al servicio la consulta de todas las citas asignadas al odontólogo con ese ID
        // ResponseEntity.ok() responde 200 OK con la lista de citas del odontólogo
        return ResponseEntity.ok(citaService.listarPorOdontologo(id));
    }

    // @PostMapping("/agendar") mapea POST /api/citas/agendar (el paciente agenda su propia cita)
    @PostMapping("/agendar")
    // Solo el paciente puede agendar su propia cita a través de este endpoint
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    // Authentication auth: identidad del paciente para asociar la cita a su perfil
    // @Valid + @RequestBody: valida y deserializa el JSON con odontólogo, fecha y servicio deseado
    public ResponseEntity<CitaResponse> agendar(Authentication auth, @Valid @RequestBody AgendarCitaRequest request) {
        // Delega al servicio la creación de la cita verificando disponibilidad del odontólogo en la fecha solicitada
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso (cita)
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.agendarCita(auth.getName(), request));
    }

    // @GetMapping("/rango") mapea GET /api/citas/rango?inicio=2024-06-01T00:00:00&fin=2024-06-30T23:59:59
    @GetMapping("/rango")
    // Todos los roles pueden consultar citas por rango de fechas
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    // @RequestParam @DateTimeFormat: extrae inicio y fin de la URL en formato ISO datetime (yyyy-MM-dd'T'HH:mm:ss)
    // y los convierte automáticamente a LocalDateTime
    public ResponseEntity<List<CitaResponse>> porRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        // Delega al servicio la consulta de todas las citas cuya fecha esté entre inicio y fin
        // ResponseEntity.ok() responde 200 OK con la lista de citas en ese rango de fechas
        return ResponseEntity.ok(citaService.listarPorRangoFecha(inicio, fin));
    }

    // @GetMapping("/{id}") mapea GET /api/citas/{id} (detalle de una cita específica)
    @GetMapping("/{id}")
    // Todos los roles pueden consultar el detalle de una cita por su ID
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA','ROLE_PACIENTE')")
    // @PathVariable Long id: extrae el ID de la cita de la URL
    public ResponseEntity<CitaResponse> obtener(@PathVariable Long id) {
        // Delega al servicio la búsqueda de la cita con ese ID; lanza excepción si no existe
        // ResponseEntity.ok() responde 200 OK con los datos completos de la cita
        return ResponseEntity.ok(citaService.obtener(id));
    }

    // @PostMapping mapea POST /api/citas (creación de cita por el personal administrativo)
    @PostMapping
    // Solo ADMIN o RECEPCIONISTA pueden crear citas manualmente en nombre de otros
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @Valid + @RequestBody CitaRequest: valida y deserializa los datos de la nueva cita (paciente, odontólogo, fecha, etc.)
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaRequest request) {
        // Delega al servicio la creación de la cita con todos los datos del request
        // ResponseEntity.status(CREATED) responde 201 CREATED porque se crea un nuevo recurso
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crear(request));
    }

    // @PutMapping("/{id}") mapea PUT /api/citas/{id} (actualización completa de una cita)
    @PutMapping("/{id}")
    // Solo ADMIN o RECEPCIONISTA pueden modificar una cita completa
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @PathVariable Long id: ID de la cita a actualizar
    // @Valid + @RequestBody CitaRequest: valida y deserializa los nuevos datos de la cita
    public ResponseEntity<CitaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        // Delega al servicio la actualización completa de la cita con los nuevos datos
        // ResponseEntity.ok() responde 200 OK con la cita actualizada
        return ResponseEntity.ok(citaService.actualizar(id, request));
    }

    // @PatchMapping("/{id}/estado") mapea PATCH /api/citas/{id}/estado?estado=CONFIRMADA
    // PATCH se usa para actualización parcial (solo se modifica el estado de la cita)
    @PatchMapping("/{id}/estado")
    // Personal clínico y administrativo puede cambiar el estado de una cita
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ODONTOLOGO','ROLE_RECEPCIONISTA')")
    // @PathVariable Long id: ID de la cita cuyo estado se quiere cambiar
    // @RequestParam String estado: nuevo estado de la cita (ej. CONFIRMADA, COMPLETADA, CANCELADA)
    public ResponseEntity<CitaResponse> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        // Delega al servicio el cambio de estado de la cita; el servicio valida que el estado sea válido
        // ResponseEntity.ok() responde 200 OK con la cita actualizada con el nuevo estado
        return ResponseEntity.ok(citaService.cambiarEstado(id, estado));
    }

    // @DeleteMapping("/{id}") mapea DELETE /api/citas/{id} (cancela/elimina una cita)
    @DeleteMapping("/{id}")
    // Solo ADMIN o RECEPCIONISTA pueden eliminar/cancelar citas desde este endpoint administrativo
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPCIONISTA')")
    // @PathVariable Long id: ID de la cita a cancelar o eliminar
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        // Delega al servicio la cancelación o eliminación de la cita con ese ID
        citaService.eliminar(id);
        // ResponseEntity.noContent().build() responde 204 NO CONTENT: operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}

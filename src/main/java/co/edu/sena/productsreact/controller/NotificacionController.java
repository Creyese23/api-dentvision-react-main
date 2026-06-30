package co.edu.sena.productsreact.controller;

// Importa la entidad Notificacion para devolverla directamente en los endpoints
import co.edu.sena.productsreact.entity.Notificacion;
// Importa la entidad User para buscar el usuario autenticado
import co.edu.sena.productsreact.entity.User;
// Repositorio de notificaciones: provee los métodos de consulta y actualización
import co.edu.sena.productsreact.repository.NotificacionRepository;
// Repositorio de usuarios: permite buscar el usuario por su email extraído del JWT
import co.edu.sena.productsreact.repository.UserRepository;
// Lombok: genera el constructor con todos los campos final (inyección por constructor)
import lombok.RequiredArgsConstructor;
// Spring: permite controlar el código de estado HTTP y el cuerpo de la respuesta
import org.springframework.http.ResponseEntity;
// Spring Security: permite declarar restricciones de acceso a nivel de método mediante expresiones SpEL
import org.springframework.security.access.prepost.PreAuthorize;
// Spring Security: punto de acceso al contexto de seguridad de la petición actual
import org.springframework.security.core.context.SecurityContextHolder;
// Spring: anotaciones para crear controladores REST y mapear rutas HTTP
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// @RestController: combina @Controller y @ResponseBody; serializa todas las respuestas a JSON automáticamente
@RestController
// @RequestMapping: todas las rutas de este controlador comienzan con /api/notificaciones
@RequestMapping("/api/notificaciones")
// @RequiredArgsConstructor: Lombok genera el constructor que inyecta los repositorios
@RequiredArgsConstructor
// @PreAuthorize a nivel de clase: cualquier endpoint requiere que el usuario esté autenticado con JWT válido
@PreAuthorize("isAuthenticated()")
public class NotificacionController {

    // Repositorio para consultar y actualizar notificaciones en la base de datos
    private final NotificacionRepository notificacionRepository;
    // Repositorio para buscar el usuario autenticado a partir de su email extraído del JWT
    private final UserRepository userRepository;

    /**
     * Obtiene el usuario actualmente autenticado extrayendo su email del contexto de seguridad.
     * El email se obtiene del JWT que Spring Security ya validó antes de llegar al controlador.
     *
     * @return La entidad User del usuario autenticado
     * @throws RuntimeException si el usuario no se encuentra (no debería ocurrir si el JWT es válido)
     */
    private User getUsuarioAutenticado() {
        // Extrae el email (principal) del JWT validado por Spring Security
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Busca y devuelve el usuario; si no existe lanza excepción (caso extremo: usuario eliminado con JWT activo)
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado: " + email));
    }

    /**
     * GET /api/notificaciones/mis
     * Devuelve las últimas 20 notificaciones del usuario autenticado, ordenadas de la más reciente a la más antigua.
     * Usado por el frontend para mostrar el panel/dropdown de notificaciones.
     *
     * @return Lista de hasta 20 notificaciones del usuario autenticado
     */
    @GetMapping("/mis")
    public ResponseEntity<List<Notificacion>> getMisNotificaciones() {
        User usuario = getUsuarioAutenticado();
        // Obtiene todas las notificaciones ordenadas por fecha descendente y limita a las 20 más recientes
        List<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioIdOrderByCreadaEnDesc(usuario.getId())
                .stream()
                .limit(20)
                .toList();
        return ResponseEntity.ok(notificaciones);
    }

    /**
     * GET /api/notificaciones/mis/no-leidas
     * Devuelve el conteo de notificaciones no leídas del usuario autenticado.
     * Usado para mostrar el badge numérico (ej: "3") sobre el ícono de campana en el navbar.
     *
     * @return JSON con el campo "count" que indica cuántas notificaciones no han sido leídas
     */
    @GetMapping("/mis/no-leidas")
    public ResponseEntity<Map<String, Long>> getCountNoLeidas() {
        User usuario = getUsuarioAutenticado();
        // Cuenta directamente en BD para garantizar precisión sin cargar todos los objetos en memoria
        long count = notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * PATCH /api/notificaciones/marcar-todas-leidas
     * Marca todas las notificaciones no leídas del usuario autenticado como leídas en una sola operación SQL.
     * Se llama cuando el usuario abre el panel de notificaciones o hace clic en "Marcar todas como leídas".
     *
     * @return JSON con mensaje de confirmación
     */
    @PatchMapping("/marcar-todas-leidas")
    @Transactional
    public ResponseEntity<Map<String, String>> marcarTodasLeidas() {
        User usuario = getUsuarioAutenticado();
        // Ejecuta un UPDATE en BD que afecta solo las notificaciones del usuario actual
        notificacionRepository.marcarTodasLeidasPorUsuarioId(usuario.getId());
        return ResponseEntity.ok(Map.of("mensaje", "Todas las notificaciones han sido marcadas como leídas"));
    }
}

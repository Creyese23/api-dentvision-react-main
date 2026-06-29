package co.edu.sena.productsreact.service;

// Importaciones de DTOs para los servicios odontológicos
import co.edu.sena.productsreact.dto.servicio.ServicioRequest;  // DTO con los datos de entrada para crear o actualizar un servicio
import co.edu.sena.productsreact.dto.servicio.ServicioResponse; // DTO con los datos que se devuelven al cliente sobre un servicio

// Entidad JPA que representa la tabla "servicios" en la base de datos
import co.edu.sena.productsreact.entity.Servicio;

// Excepciones personalizadas del proyecto
import co.edu.sena.productsreact.exception.DuplicateResourceException; // Se lanza cuando ya existe un servicio con el mismo nombre
import co.edu.sena.productsreact.exception.ResourceNotFoundException;  // Se lanza cuando no se encuentra el servicio solicitado

// Repositorio JPA para acceder a la tabla de servicios
import co.edu.sena.productsreact.repository.ServicioRepository;

import lombok.RequiredArgsConstructor; // Genera el constructor con inyección de dependencias automáticamente
import org.springframework.stereotype.Service;                    // Marca la clase como componente de servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para controlar transacciones en operaciones de escritura

import java.util.List; // Colección de elementos

// @Service: Spring registra esta clase en su contenedor como un bean de lógica de negocio
@Service
// @RequiredArgsConstructor: Lombok genera el constructor que inyecta el ServicioRepository
@RequiredArgsConstructor
// @Transactional(readOnly = true): por defecto las operaciones de este servicio solo leen la BD
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ServicioService {

    // Repositorio para consultar y persistir servicios odontológicos en la BD
    private final ServicioRepository servicioRepository;

    /**
     * Devuelve la lista de todos los servicios activos (estado=true).
     * Se usa para mostrar el catálogo de servicios disponibles en la clínica.
     */
    public List<ServicioResponse> listarActivos() {
        // Busca solo servicios con estado=true y convierte cada entidad a su DTO de respuesta
        return servicioRepository.findByEstadoTrue().stream()
                .map(ServicioResponse::from).toList();
    }

    /**
     * Obtiene un servicio específico por su ID.
     * Lanza ResourceNotFoundException si no existe.
     */
    public ServicioResponse obtener(Long id) {
        // getOrThrow es un método auxiliar que busca y lanza excepción si no encuentra
        return ServicioResponse.from(getOrThrow(id));
    }

    @Transactional // Esta operación escribe en la BD (INSERT)
    /**
     * Crea un nuevo servicio odontológico.
     * Verifica que no exista otro con el mismo nombre para evitar duplicados.
     */
    public ServicioResponse crear(ServicioRequest req) {
        // Verifica que no exista ya un servicio con ese nombre (unicidad de nombre)
        if (servicioRepository.existsByNombre(req.nombre())) {
            throw new DuplicateResourceException("Ya existe el servicio '" + req.nombre() + "'");
        }
        // Construye la entidad Servicio con los datos del request usando el patrón Builder
        Servicio s = Servicio.builder()
                .nombre(req.nombre())           // Nombre del servicio (ej. "Limpieza dental")
                .descripcion(req.descripcion()) // Descripción detallada del servicio
                // Si el precio no se envía (null), se asigna cero como valor predeterminado
                .precio(req.precio() != null ? req.precio() : java.math.BigDecimal.ZERO)
                .imagenUrl(req.imagenUrl())     // URL de la imagen ilustrativa del servicio
                .estado(true)                   // El servicio queda activo al crearse
                .build();
        // Persiste el nuevo servicio en la BD y retorna su DTO
        return ServicioResponse.from(servicioRepository.save(s));
    }

    @Transactional // Esta operación escribe en la BD (UPDATE)
    /**
     * Actualiza los datos de un servicio existente.
     * Solo actualiza precio e imagenUrl si vienen con valor en el request.
     */
    public ServicioResponse actualizar(Long id, ServicioRequest req) {
        // Obtiene el servicio existente; lanza excepción si no existe
        Servicio s = getOrThrow(id);
        s.setNombre(req.nombre());           // Actualiza el nombre del servicio
        s.setDescripcion(req.descripcion()); // Actualiza la descripción
        // Solo actualiza el precio si el request trae un valor no nulo (evita borrar el precio existente)
        if (req.precio() != null) s.setPrecio(req.precio());
        // Solo actualiza la URL de imagen si el request trae una nueva URL
        if (req.imagenUrl() != null) s.setImagenUrl(req.imagenUrl());
        // Persiste los cambios y retorna el DTO actualizado
        return ServicioResponse.from(servicioRepository.save(s));
    }

    @Transactional // Esta operación escribe en la BD (UPDATE - eliminación lógica)
    /**
     * Desactiva un servicio (eliminación lógica).
     * El registro permanece en la BD pero ya no aparece en el catálogo activo.
     */
    public void desactivar(Long id) {
        // Obtiene el servicio; lanza excepción si no existe
        Servicio s = getOrThrow(id);
        s.setEstado(false); // Marca el servicio como inactivo (no se borra físicamente)
        servicioRepository.save(s); // Persiste el cambio en la BD
    }

    /**
     * Método auxiliar público: busca un servicio por ID y lanza excepción si no existe.
     * Es público porque otros servicios (ej. FacturaService) también lo necesitan.
     */
    public Servicio getOrThrow(Long id) {
        return servicioRepository.findById(id)
                // Si no se encuentra el servicio, lanza ResourceNotFoundException con mensaje descriptivo
                .orElseThrow(() -> new ResourceNotFoundException("Servicio " + id + " no encontrado"));
    }
}

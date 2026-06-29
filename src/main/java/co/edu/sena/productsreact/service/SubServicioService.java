package co.edu.sena.productsreact.service;

// Importaciones de DTOs para sub-servicios odontológicos
import co.edu.sena.productsreact.dto.servicio.SubServicioRequest;  // DTO con los datos para crear o actualizar un sub-servicio
import co.edu.sena.productsreact.dto.servicio.SubServicioResponse; // DTO con los datos del sub-servicio que se devuelven al cliente

// Entidades JPA que representan tablas en la BD
import co.edu.sena.productsreact.entity.Servicio;    // Entidad del servicio padre al que pertenece el sub-servicio
import co.edu.sena.productsreact.entity.SubServicio; // Entidad del sub-servicio (variante específica de un servicio)

// Excepción cuando no se encuentra un recurso
import co.edu.sena.productsreact.exception.ResourceNotFoundException;

// Repositorios para acceder a las tablas correspondientes
import co.edu.sena.productsreact.repository.ServicioRepository;    // Repositorio para buscar el servicio padre
import co.edu.sena.productsreact.repository.SubServicioRepository; // Repositorio para gestionar sub-servicios

import lombok.RequiredArgsConstructor; // Genera constructor con inyección de dependencias
import org.springframework.stereotype.Service;                    // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Controla transacciones de BD

import java.util.List; // Colección de elementos

// @Service: registra esta clase en el contenedor de Spring como componente de servicio
@Service
// @RequiredArgsConstructor: Lombok inyecta los dos repositorios por constructor
@RequiredArgsConstructor
// @Transactional(readOnly = true): las consultas de solo lectura son la mayoría aquí
@Transactional(readOnly = true)
public class SubServicioService {

    // Repositorio para consultar y persistir sub-servicios en la BD
    private final SubServicioRepository subServicioRepository;
    // Repositorio para verificar que el servicio padre exista antes de crear un sub-servicio
    private final ServicioRepository servicioRepository;

    /**
     * Lista todos los sub-servicios activos de un servicio padre específico.
     * Por ejemplo: para "Ortodoncia" puede listar "Brackets metálicos", "Brackets estéticos", etc.
     */
    public List<SubServicioResponse> listarPorServicio(Long servicioId) {
        // Busca los sub-servicios por el ID del servicio padre y que estén activos (estado=true)
        return subServicioRepository.findByServicioIdAndEstadoTrue(servicioId)
                .stream().map(SubServicioResponse::from).toList(); // Convierte cada entidad a DTO
    }

    @Transactional // Esta operación crea un registro en la BD (INSERT)
    /**
     * Crea un nuevo sub-servicio asociado a un servicio padre.
     * Primero verifica que el servicio padre exista.
     */
    public SubServicioResponse crear(Long servicioId, SubServicioRequest req) {
        // Busca el servicio padre por ID; si no existe lanza ResourceNotFoundException
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio " + servicioId + " no encontrado"));
        // Construye el sub-servicio con los datos del request usando el patrón Builder
        SubServicio sub = SubServicio.builder()
                .servicio(servicio)               // Asocia el sub-servicio al servicio padre encontrado
                .nombre(req.nombre())             // Nombre del sub-servicio (ej. "Brackets metálicos")
                .descripcion(req.descripcion())   // Descripción detallada de este sub-servicio
                .precio(req.precio())             // Precio específico de este sub-servicio
                .duracionMinutos(req.duracionMinutos()) // Duración estimada del procedimiento en minutos
                .estado(true)                     // El sub-servicio queda activo al crearse
                .build();
        // Guarda en la BD y retorna el DTO del sub-servicio creado
        return SubServicioResponse.from(subServicioRepository.save(sub));
    }

    @Transactional // Esta operación actualiza un registro en la BD (UPDATE)
    /**
     * Actualiza los datos de un sub-servicio existente.
     * Lanza excepción si el sub-servicio no existe.
     */
    public SubServicioResponse actualizar(Long id, SubServicioRequest req) {
        // Busca el sub-servicio por ID; si no existe lanza ResourceNotFoundException
        SubServicio sub = subServicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-servicio " + id + " no encontrado"));
        sub.setNombre(req.nombre());                         // Actualiza el nombre del sub-servicio
        sub.setDescripcion(req.descripcion());               // Actualiza la descripción
        sub.setPrecio(req.precio());                         // Actualiza el precio
        sub.setDuracionMinutos(req.duracionMinutos());       // Actualiza la duración en minutos
        // Guarda los cambios y retorna el DTO actualizado
        return SubServicioResponse.from(subServicioRepository.save(sub));
    }

    @Transactional // Esta operación hace una eliminación lógica en la BD (UPDATE)
    /**
     * Desactiva un sub-servicio (eliminación lógica).
     * El registro permanece en la BD pero no aparece en el catálogo activo.
     */
    public void eliminar(Long id) {
        // Busca el sub-servicio; lanza excepción si no existe
        SubServicio sub = subServicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-servicio " + id + " no encontrado"));
        sub.setEstado(false); // Marca como inactivo sin borrar el registro físicamente de la BD
        subServicioRepository.save(sub); // Persiste el cambio de estado en la BD
    }
}

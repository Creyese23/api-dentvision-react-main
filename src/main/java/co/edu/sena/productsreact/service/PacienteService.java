package co.edu.sena.productsreact.service;

// Importaciones de DTOs (Data Transfer Objects) para recibir y devolver datos del paciente
import co.edu.sena.productsreact.dto.paciente.PacienteRequest;   // DTO con los datos que llegan desde el cliente al crear/actualizar un paciente
import co.edu.sena.productsreact.dto.paciente.PacienteResponse;  // DTO con los datos que se envían de vuelta al cliente sobre un paciente
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;    // DTO con los datos básicos de un usuario (email, rol, etc.)

// Importaciones de entidades JPA (representan tablas en la base de datos)
import co.edu.sena.productsreact.entity.Paciente;        // Entidad que representa la tabla "pacientes" en la BD
import co.edu.sena.productsreact.entity.PerfilUsuario;   // Entidad que guarda datos personales del usuario (nombre, teléfono, etc.)
import co.edu.sena.productsreact.entity.User;            // Entidad que representa un usuario del sistema con email y contraseña

// Importaciones de excepciones personalizadas del proyecto
import co.edu.sena.productsreact.exception.DuplicateResourceException;  // Se lanza cuando se intenta crear un recurso que ya existe
import co.edu.sena.productsreact.exception.ResourceNotFoundException;   // Se lanza cuando no se encuentra un recurso en la BD

// Importaciones de repositorios JPA (permiten hacer consultas a la base de datos)
import co.edu.sena.productsreact.repository.PacienteRepository;      // Repositorio para consultas sobre la tabla pacientes
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;  // Repositorio para consultas sobre perfiles de usuario
import co.edu.sena.productsreact.repository.UserRepository;           // Repositorio para consultas sobre la tabla de usuarios

import lombok.RequiredArgsConstructor; // Anotación de Lombok que genera automáticamente el constructor con todos los campos final

// Importaciones de Spring para paginación y manejo de colecciones
import org.springframework.data.domain.Page;     // Representa una página de resultados (con número de página, tamaño, total, etc.)
import org.springframework.data.domain.Pageable; // Parámetros de paginación enviados desde el cliente (página, tamaño, orden)
import org.springframework.stereotype.Service;   // Marca esta clase como un servicio de Spring (capa de lógica de negocio)
import org.springframework.transaction.annotation.Transactional; // Permite controlar transacciones de base de datos

import java.time.LocalDateTime; // Clase para manejar fecha y hora sin zona horaria
import java.util.List;          // Interfaz de colección para listas de elementos

// @Service: indica a Spring que esta clase es un componente de servicio (lógica de negocio)
// Spring la registra como un bean y la gestiona en su contenedor
@Service
// @RequiredArgsConstructor: Lombok genera el constructor con los tres repositorios (inyección de dependencias por constructor)
@RequiredArgsConstructor
// @Transactional(readOnly = true): todas las operaciones de esta clase son de solo lectura por defecto,
// lo que mejora el rendimiento al evitar bloqueos innecesarios en la BD
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class PacienteService {

    // Repositorio para acceder a la tabla de pacientes; inyectado automáticamente por Spring
    private final PacienteRepository pacienteRepository;
    // Repositorio para acceder a la tabla de usuarios; se usa para buscar el usuario vinculado al paciente
    private final UserRepository userRepository;
    // Repositorio para acceder y guardar el perfil personal del usuario (nombres, teléfono, etc.)
    private final PerfilUsuarioRepository perfilRepository;

    /**
     * Devuelve la lista de todos los pacientes que están activos (estado=true) y no han sido eliminados lógicamente.
     * Se usa para mostrar el listado de pacientes en el panel administrativo.
     */
    public List<PacienteResponse> listarActivos() {
        // Busca en la BD todos los pacientes con estado=true y sin fecha de eliminación,
        // luego transforma cada entidad Paciente en un PacienteResponse usando el método estático PacienteResponse::from
        return pacienteRepository.findByEstadoTrueAndFechaEliminacionIsNull().stream()
                .map(PacienteResponse::from).toList();
    }

    /**
     * Busca pacientes por texto (nombre, identificación, etc.) con soporte de paginación.
     * El parámetro "q" es la cadena de búsqueda que el usuario escribe en el buscador.
     */
    public Page<PacienteResponse> buscar(String q, Pageable pageable) {
        // Delega la búsqueda al repositorio con la consulta personalizada "buscar",
        // luego mapea cada resultado a PacienteResponse
        return pacienteRepository.buscar(q, pageable).map(PacienteResponse::from);
    }

    /**
     * Obtiene un paciente específico por su ID.
     * Lanza excepción si el paciente no existe.
     */
    public PacienteResponse obtener(Long id) {
        // getOrThrow busca el paciente y lanza ResourceNotFoundException si no lo encuentra
        return PacienteResponse.from(getOrThrow(id));
    }

    /**
     * Permite que un usuario con rol paciente obtenga su propio perfil de paciente,
     * buscando por el email del usuario autenticado.
     */
    public PacienteResponse obtenerMiPaciente(String email) {
        // Busca el usuario por su email; si no existe lanza ResourceNotFoundException
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        // Busca el paciente vinculado a ese usuario usando su ID
        Paciente p = pacienteRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes un perfil de paciente registrado"));
        // Convierte la entidad Paciente a DTO y lo retorna
        return PacienteResponse.from(p);
    }

    /**
     * Lista los usuarios con rol PACIENTE que aún no tienen un perfil de paciente creado.
     * Útil para que el administrador active nuevos pacientes.
     */
    public List<UsuarioResponse> listarPendientesActivacion() {
        // Busca todos los usuarios activos con rol "ROLE_PACIENTE"
        return userRepository.findByRolNombreRolAndEstadoTrue("ROLE_PACIENTE").stream()
                // Filtra solo aquellos que NO tienen un registro en la tabla de pacientes
                .filter(u -> !pacienteRepository.existsByUsuarioId(u.getId()))
                // Convierte cada usuario a UsuarioResponse
                .map(UsuarioResponse::from).toList();
    }

    // @Transactional sin readOnly: esta operación modifica la BD (INSERT), por eso necesita transacción de escritura
    @Transactional
    /**
     * Activa un usuario como paciente creando su registro en la tabla de pacientes.
     * Solo el administrador puede invocar este método.
     */
    public PacienteResponse activarPaciente(Long idUsuario) {
        // Busca el usuario por ID; si no existe lanza ResourceNotFoundException con el ID en el mensaje
        User user = userRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + idUsuario + " no encontrado"));
        // Verifica que el usuario no tenga ya un perfil de paciente para evitar duplicados
        if (pacienteRepository.existsByUsuarioId(idUsuario)) {
            // Lanza excepción personalizada indicando que ya existe el paciente
            throw new DuplicateResourceException("El usuario ya tiene un perfil de paciente activo");
        }
        // Construye la entidad Paciente usando el patrón Builder de Lombok,
        // vinculándola al usuario y marcándola como activa
        Paciente p = Paciente.builder()
                .usuario(user)   // Asocia el paciente al usuario encontrado
                .estado(true)    // El paciente está activo desde el inicio
                .build();
        // Guarda el nuevo paciente en la BD y retorna su representación como DTO
        return PacienteResponse.from(pacienteRepository.save(p));
    }

    @Transactional
    /**
     * Crea un paciente completo con sus datos personales (perfil) y fecha de nacimiento.
     * También crea o actualiza el PerfilUsuario asociado.
     */
    public PacienteResponse crear(PacienteRequest req) {
        // Busca el usuario al que se vinculará el paciente usando el ID que viene en el request
        User usuario = userRepository.findById(req.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + req.idUsuario() + " no encontrado"));

        // Verifica que el usuario no tenga ya un paciente vinculado; si lo tiene, lanza excepción
        if (pacienteRepository.existsByUsuarioId(req.idUsuario())) {
            throw new DuplicateResourceException("Ya existe un paciente vinculado al usuario " + req.idUsuario());
        }
        // Verifica que la identificación no esté ya registrada en otro perfil
        // (solo si se envió una identificación no vacía)
        if (req.identificacion() != null && !req.identificacion().isBlank()
                && perfilRepository.existsByIdentificacion(req.identificacion())) {
            throw new DuplicateResourceException("Ya existe un perfil con identificación " + req.identificacion());
        }

        // Obtiene el perfil personal del usuario; si no existe, crea uno nuevo vacío
        PerfilUsuario perfil = usuario.getPerfil();
        if (perfil == null) {
            // Crea un nuevo PerfilUsuario si el usuario aún no tiene uno
            perfil = new PerfilUsuario();
            perfil.setUsuario(usuario); // Vincula el perfil al usuario
        }
        // Asigna los datos personales al perfil; si algún campo llega null, se guarda como cadena vacía
        perfil.setNombres(req.nombres() != null ? req.nombres() : "");                               // Nombre(s) del paciente
        perfil.setApellidos(req.apellidos() != null ? req.apellidos() : "");                         // Apellido(s) del paciente
        perfil.setIdentificacion(req.identificacion() != null ? req.identificacion() : "");          // Número de documento (cédula, pasaporte, etc.)
        perfil.setTipoIdentificacion(req.tipoIdentificacion() != null ? req.tipoIdentificacion() : ""); // Tipo de documento (CC, TI, CE, etc.)
        perfil.setTelefono(req.telefono() != null ? req.telefono() : "");                            // Número telefónico de contacto
        perfil.setDireccion(req.direccion() != null ? req.direccion() : "");                         // Dirección de residencia del paciente
        perfilRepository.save(perfil); // Persiste el perfil actualizado en la BD

        // Construye y guarda la entidad Paciente con la fecha de nacimiento y el usuario vinculado
        Paciente p = Paciente.builder()
                .usuario(usuario)                      // Usuario dueño de este perfil de paciente
                .fechaNacimiento(req.fechaNacimiento()) // Fecha de nacimiento para calcular edad, etc.
                .estado(true)                          // El paciente queda activo al crearse
                .build();
        // Guarda el paciente en la BD y retorna su DTO
        return PacienteResponse.from(pacienteRepository.save(p));
    }

    @Transactional
    /**
     * Actualiza los datos de un paciente existente (fecha de nacimiento y datos del perfil).
     */
    public PacienteResponse actualizar(Long id, PacienteRequest req) {
        // Obtiene el paciente existente; lanza excepción si no se encuentra
        Paciente p = getOrThrow(id);
        // Actualiza la fecha de nacimiento del paciente
        p.setFechaNacimiento(req.fechaNacimiento());

        // Si el paciente tiene un usuario vinculado, actualiza también su perfil personal
        if (p.getUsuario() != null) {
            // Obtiene el perfil del usuario; si no existe, crea uno nuevo
            PerfilUsuario perfil = p.getUsuario().getPerfil();
            if (perfil == null) {
                // El usuario no tiene perfil aún, se crea uno nuevo y se vincula
                perfil = new PerfilUsuario();
                perfil.setUsuario(p.getUsuario());
            }
            // Actualiza cada campo del perfil; si llega null, se guarda como cadena vacía para evitar nulls en BD
            perfil.setNombres(req.nombres() != null ? req.nombres() : "");
            perfil.setApellidos(req.apellidos() != null ? req.apellidos() : "");
            perfil.setIdentificacion(req.identificacion() != null ? req.identificacion() : "");
            perfil.setTipoIdentificacion(req.tipoIdentificacion() != null ? req.tipoIdentificacion() : "");
            perfil.setTelefono(req.telefono() != null ? req.telefono() : "");
            perfil.setDireccion(req.direccion() != null ? req.direccion() : "");
            perfilRepository.save(perfil); // Persiste los cambios del perfil en la BD
        }
        // Guarda el paciente actualizado y retorna el DTO con los nuevos datos
        return PacienteResponse.from(pacienteRepository.save(p));
    }

    @Transactional
    /**
     * Realiza la eliminación lógica de un paciente (no se borra de la BD, solo se marca como inactivo).
     * Esto preserva el historial del paciente para auditoría.
     */
    public void eliminar(Long id) {
        // Obtiene el paciente; lanza excepción si no existe
        Paciente p = getOrThrow(id);
        // Marca el paciente como inactivo (eliminación lógica)
        p.setEstado(false);
        // Registra la fecha y hora exacta en que se eliminó el paciente
        p.setFechaEliminacion(LocalDateTime.now());
        // Persiste los cambios en la BD (el registro no se borra físicamente)
        pacienteRepository.save(p);
    }

    /**
     * Método auxiliar privado: busca un paciente por ID y lanza ResourceNotFoundException si no existe.
     * Se reutiliza en todos los métodos que necesitan obtener un paciente antes de operar sobre él.
     */
    private Paciente getOrThrow(Long id) {
        return pacienteRepository.findById(id)
                // Si el Optional está vacío, lanza la excepción con un mensaje descriptivo
                .orElseThrow(() -> new ResourceNotFoundException("Paciente " + id + " no encontrado"));
    }
}

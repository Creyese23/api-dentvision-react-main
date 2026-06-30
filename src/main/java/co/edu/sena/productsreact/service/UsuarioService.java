package co.edu.sena.productsreact.service;

// Importaciones de DTOs para crear y responder con datos de usuario
import co.edu.sena.productsreact.dto.usuario.CreateUsuarioRequest; // DTO con los datos necesarios para crear un nuevo usuario (email, contraseña, rol)
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;      // DTO con los datos del usuario que se devuelven al cliente

// Importaciones de entidades JPA que representan tablas en la base de datos
import co.edu.sena.productsreact.entity.Empleado;       // Entidad que representa a un empleado de la clínica
import co.edu.sena.productsreact.entity.Notificacion;   // Entidad de notificación in-app
import co.edu.sena.productsreact.entity.Paciente;       // Entidad que representa a un paciente
import co.edu.sena.productsreact.entity.PerfilUsuario;  // Entidad con los datos personales del usuario (nombre, teléfono, etc.)
import co.edu.sena.productsreact.entity.Rol;            // Entidad que representa un rol del sistema (ADMIN, PACIENTE, EMPLEADO, etc.)
import co.edu.sena.productsreact.entity.User;           // Entidad principal del usuario (email, contraseña, rol)

// Excepciones personalizadas del proyecto
import co.edu.sena.productsreact.exception.DuplicateResourceException; // Se lanza cuando ya existe un recurso con los mismos datos únicos
import co.edu.sena.productsreact.exception.ResourceNotFoundException;  // Se lanza cuando no se encuentra el recurso solicitado

// Repositorios para acceder a cada tabla de la base de datos
import co.edu.sena.productsreact.repository.EmpleadoRepository;       // Consultas sobre la tabla de empleados
import co.edu.sena.productsreact.repository.NotificacionRepository;   // Repositorio para guardar notificaciones in-app
import co.edu.sena.productsreact.repository.PacienteRepository;       // Consultas sobre la tabla de pacientes
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;  // Consultas sobre la tabla de perfiles de usuario
import co.edu.sena.productsreact.repository.RefreshTokenRepository;   // Consultas sobre la tabla de refresh tokens (sesiones activas)
import co.edu.sena.productsreact.repository.RolRepository;            // Consultas sobre la tabla de roles
import co.edu.sena.productsreact.repository.UserRepository;           // Consultas sobre la tabla de usuarios

import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor; // Genera el constructor con todos los campos final automáticamente
import org.springframework.security.crypto.password.PasswordEncoder; // Interfaz para cifrar y verificar contraseñas (BCrypt)
import org.springframework.stereotype.Service;                       // Marca la clase como servicio de Spring
import org.springframework.transaction.annotation.Transactional;    // Controla las transacciones de base de datos

import java.time.LocalDateTime; // Para registrar fecha y hora de eliminaciones lógicas
import java.util.List;          // Colección de elementos

// @Service: Spring registra esta clase como bean de servicio (capa de lógica de negocio)
@Service
// @RequiredArgsConstructor: Lombok inyecta todos los campos final por constructor (buena práctica vs @Autowired)
@RequiredArgsConstructor
// @Transactional(readOnly = true): por defecto todas las operaciones solo leen la BD (mejor rendimiento)
@Transactional(readOnly = true)
@Slf4j
public class UsuarioService {

    // Repositorio principal para buscar, crear y actualizar usuarios en la BD
    private final UserRepository userRepository;
    // Repositorio para buscar y crear roles (ADMIN, PACIENTE, ODONTOLOGO, TECNICO, etc.)
    private final RolRepository rolRepository;
    // Repositorio para guardar el perfil personal del usuario (nombres, apellidos, teléfono, dirección)
    private final PerfilUsuarioRepository perfilRepository;
    // Repositorio para verificar y gestionar registros de paciente asociados al usuario
    private final PacienteRepository pacienteRepository;
    // Repositorio para verificar y gestionar registros de empleado asociados al usuario
    private final EmpleadoRepository empleadoRepository;
    // Repositorio para revocar los refresh tokens cuando cambia el rol del usuario (invalida sesiones activas)
    private final RefreshTokenRepository refreshTokenRepository;
    // Codificador de contraseñas (BCrypt): cifra la contraseña antes de guardarla en la BD
    private final PasswordEncoder passwordEncoder;
    // Repositorio para guardar notificaciones in-app
    private final NotificacionRepository notificacionRepository;

    /**
     * Devuelve la lista de todos los usuarios activos del sistema.
     * Solo incluye usuarios con estado=true (no eliminados).
     */
    public List<UsuarioResponse> listar() {
        // Obtiene todos los usuarios, filtra los que tienen estado=true y los convierte a DTO
        return userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEstado())) // Excluye usuarios desactivados
                .map(UsuarioResponse::from).toList();             // Convierte cada User a UsuarioResponse
    }

    /**
     * Devuelve la lista de TODOS los usuarios, incluyendo los desactivados.
     * Útil para auditoría o reportes administrativos.
     */
    public List<UsuarioResponse> listarTodos() {
        // Sin filtro: devuelve absolutamente todos los usuarios de la BD
        return userRepository.findAll().stream()
                .map(UsuarioResponse::from).toList();
    }

    @Transactional // Escritura en BD: cambia el estado del usuario
    /**
     * Activa o desactiva un usuario (toggle de estado).
     * Si estaba activo lo desactiva, y viceversa.
     */
    public UsuarioResponse toggleEstado(Long id) {
        // Busca el usuario por ID; lanza excepción si no existe
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " no encontrado"));
        // Invierte el estado actual: si era true pasa a false, si era false pasa a true
        // Se usa Boolean.TRUE.equals para manejar de forma segura el caso en que getEstado() sea null
        user.setEstado(!Boolean.TRUE.equals(user.getEstado()));
        // Guarda el cambio y retorna el DTO con el nuevo estado
        return UsuarioResponse.from(userRepository.save(user));
    }

    @Transactional // Escritura en BD: cambia el rol y puede crear/desactivar registros de paciente o empleado
    /**
     * Cambia el rol de un usuario (ej. de PACIENTE a ODONTOLOGO).
     * También ajusta automáticamente los registros de paciente/empleado según el nuevo rol,
     * y revoca todas las sesiones activas del usuario para forzar re-login.
     */
    public UsuarioResponse cambiarRol(Long id, String nuevoRolNombre) {
        // Busca el usuario al que se le cambiará el rol
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " no encontrado"));

        // Busca el rol en la BD; si no existe, lo crea automáticamente
        // Esto permite roles dinámicos sin necesidad de crearlos manualmente primero
        Rol rol = rolRepository.findByNombreRol(nuevoRolNombre)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol(nuevoRolNombre).build()));
        user.setRol(rol); // Asigna el nuevo rol al usuario
        userRepository.save(user); // Persiste el cambio de rol en la BD
        // Revoca todos los refresh tokens del usuario para invalidar sus sesiones activas
        // Esto obliga al usuario a hacer login de nuevo con el nuevo rol
        refreshTokenRepository.revokeAllByUsuarioId(id);

        // Si el nuevo rol es PACIENTE, se gestiona el registro de paciente
        if ("ROLE_PACIENTE".equals(nuevoRolNombre)) {
            // Reactivar o crear registro en pacientes
            // Si ya existe un registro de paciente (posiblemente desactivado), lo reactiva;
            // si no existe, crea uno nuevo
            pacienteRepository.findByUsuarioId(id).ifPresentOrElse(p -> {
                p.setEstado(true);            // Reactiva el registro de paciente
                p.setFechaEliminacion(null);  // Elimina la marca de eliminación lógica
                pacienteRepository.save(p);
            }, () -> pacienteRepository.save(Paciente.builder().usuario(user).estado(true).build()));

            // Desactivar empleado si existe
            // Si el usuario era empleado antes, desactiva ese registro para mantener consistencia
            empleadoRepository.findByUsuarioId(id).ifPresent(e -> {
                if (Boolean.TRUE.equals(e.getEstado())) {
                    e.setEstado(false);                          // Desactiva el registro de empleado
                    e.setFechaEliminacion(LocalDateTime.now()); // Registra la fecha de desactivación
                    empleadoRepository.save(e);
                }
            });
        } else {
            // Si el nuevo rol NO es PACIENTE, desactiva el registro de paciente si existe
            // (porque un odontologo o admin no debe tener un perfil de paciente activo)
            // Desactivar registro en pacientes si existe
            pacienteRepository.findByUsuarioId(id).ifPresent(p -> {
                if (Boolean.TRUE.equals(p.getEstado())) {
                    p.setEstado(false);                          // Desactiva el registro de paciente
                    p.setFechaEliminacion(LocalDateTime.now()); // Registra la fecha de desactivación
                    pacienteRepository.save(p);
                }
            });
        }

        // Retorna el DTO del usuario con el nuevo rol asignado
        return UsuarioResponse.from(user);
    }

    @Transactional // Escritura en BD: crea usuario y perfil
    /**
     * Crea un nuevo usuario en el sistema con su perfil personal vacío.
     * Valida que el email no esté ya registrado y que el rol exista.
     */
    public UsuarioResponse crear(CreateUsuarioRequest req) {
        // Verifica que el email no esté ya registrado en la BD para evitar duplicados
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("El email '" + req.email() + "' ya está registrado");
        }
        // Busca el rol especificado en el request; lanza excepción si el rol no existe en la BD
        Rol rol = rolRepository.findByNombreRol(req.rol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol '" + req.rol() + "' no encontrado"));

        // Construye la entidad User con los datos del request
        User user = User.builder()
                .email(req.email())                          // Email que será el nombre de usuario
                .password(passwordEncoder.encode(req.password())) // Cifra la contraseña con BCrypt antes de guardarla
                .rol(rol)                                    // Asigna el rol al usuario
                .estado(true)                               // El usuario queda activo desde que se crea
                .build();
        user = userRepository.save(user); // Persiste el usuario en la BD y obtiene el ID generado

        // Crea un PerfilUsuario vacío vinculado al usuario recién creado
        // El perfil contiene los datos personales que el usuario llenará después
        perfilRepository.save(PerfilUsuario.builder()
                .usuario(user)              // Vincula el perfil al usuario creado
                .nombres("").apellidos("")  // Campos vacíos que el usuario llenará en su perfil
                .tipoIdentificacion("").identificacion("") // Tipo y número de documento vacíos
                .telefono("").direccion("") // Datos de contacto vacíos
                .build());

        // Notificaciones in-app a todos los administradores
        try {
            final String emailNuevo = req.email();
            final String rolNuevo = req.rol();
            userRepository.findByRolNombreRolAndEstadoTrue("ROLE_ADMIN").forEach(admin -> {
                try {
                    notificacionRepository.save(Notificacion.builder()
                            .usuario(admin)
                            .tipo("INFO")
                            .mensaje("Nuevo usuario registrado: " + emailNuevo + " (" + rolNuevo + ")")
                            .build());
                } catch (Exception ex) {
                    log.warn("No se pudo guardar notificación de nuevo usuario para admin {}: {}", admin.getId(), ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("No se pudo guardar notificaciones de nuevo usuario: {}", e.getMessage());
        }

        // Retorna el DTO con los datos del usuario recién creado
        return UsuarioResponse.from(user);
    }
}

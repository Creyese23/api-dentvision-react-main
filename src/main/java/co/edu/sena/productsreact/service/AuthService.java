package co.edu.sena.productsreact.service;

// Importaciones de los DTOs (Data Transfer Objects) usados en las operaciones de autenticación
import co.edu.sena.productsreact.dto.auth.AuthResponse;    // Respuesta que contiene access token, refresh token y datos del usuario
import co.edu.sena.productsreact.dto.auth.LoginRequest;     // Datos de entrada para el inicio de sesión (email + contraseña)
import co.edu.sena.productsreact.dto.auth.RegisterRequest;  // Datos de entrada para el registro (email + contraseña)
import co.edu.sena.productsreact.dto.auth.UserDto;          // Objeto simplificado con los datos públicos del usuario

// Importaciones de las entidades JPA que se mapean a tablas de la base de datos
import co.edu.sena.productsreact.entity.Paciente;           // Entidad que representa al paciente vinculado al usuario
import co.edu.sena.productsreact.entity.PerfilUsuario;      // Entidad con los datos personales del usuario (nombres, teléfono, etc.)
import co.edu.sena.productsreact.entity.RefreshToken;       // Entidad que representa el token de renovación de sesión
import co.edu.sena.productsreact.entity.Rol;                // Entidad que representa el rol del usuario (ROLE_PACIENTE, ROLE_ADMIN, etc.)
import co.edu.sena.productsreact.entity.User;               // Entidad principal del usuario con email, contraseña cifrada y rol

// Importaciones de las excepciones personalizadas del proyecto
import co.edu.sena.productsreact.exception.DuplicateResourceException;  // Se lanza cuando ya existe un recurso con los mismos datos
import co.edu.sena.productsreact.exception.ResourceNotFoundException;    // Se lanza cuando no se encuentra un recurso solicitado

// Importaciones de los repositorios JPA para acceder a la base de datos
import co.edu.sena.productsreact.repository.PacienteRepository;        // Repositorio para operaciones CRUD sobre la tabla Paciente
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;   // Repositorio para operaciones CRUD sobre la tabla PerfilUsuario
import co.edu.sena.productsreact.repository.RefreshTokenRepository;    // Repositorio para gestionar los tokens de renovación
import co.edu.sena.productsreact.repository.RolRepository;             // Repositorio para buscar y guardar roles
import co.edu.sena.productsreact.repository.UserRepository;            // Repositorio para buscar y guardar usuarios

// Importación del servicio JWT que genera y valida los tokens de acceso
import co.edu.sena.productsreact.security.JwtService;

// Lombok: genera el constructor con todos los campos final (inyección por constructor)
import lombok.RequiredArgsConstructor;
// Lombok: habilita el logger 'log' usando SLF4J para registrar eventos en consola/archivos
import lombok.extern.slf4j.Slf4j;

// Spring: permite inyectar valores del archivo application.properties o variables de entorno
import org.springframework.beans.factory.annotation.Value;
// Spring Security: gestiona la autenticación del usuario
import org.springframework.security.authentication.AuthenticationManager;
// Spring Security: token que encapsula email y contraseña para el proceso de autenticación
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Spring Security: resultado del proceso de autenticación
import org.springframework.security.core.Authentication;
// Spring Security: interfaz que representa los datos del usuario autenticado
import org.springframework.security.core.userdetails.UserDetails;
// Spring Security: codifica (cifra) las contraseñas usando BCrypt
import org.springframework.security.crypto.password.PasswordEncoder;
// Spring: marca esta clase como un servicio de la capa de negocio (componente gestionado por Spring)
import org.springframework.stereotype.Service;
// Spring: envuelve los métodos en una transacción de base de datos; si algo falla, revierte los cambios
import org.springframework.transaction.annotation.Transactional;

// Clases estándar de Java para manejo de fechas y generación de identificadores únicos
import java.time.LocalDateTime; // Representa fecha y hora sin zona horaria
import java.util.UUID;          // Genera identificadores únicos universales (tokens aleatorios)

// @Service: indica a Spring que esta clase es un bean de la capa de servicio; se puede inyectar en controladores
@Service
// @RequiredArgsConstructor: Lombok genera un constructor con todos los campos 'final', habilitando la inyección de dependencias
@RequiredArgsConstructor
// @Slf4j: Lombok crea automáticamente el campo 'log' para registrar mensajes de información, advertencia y error
@Slf4j
public class AuthService {

    // Repositorio para consultar y guardar usuarios en la base de datos
    private final UserRepository userRepository;
    // Repositorio para consultar y crear roles (ROLE_PACIENTE, ROLE_ADMIN, etc.)
    private final RolRepository rolRepository;
    // Repositorio para guardar el perfil de usuario con datos personales
    private final PerfilUsuarioRepository perfilRepository;
    // Repositorio para crear el registro de paciente vinculado al usuario
    private final PacienteRepository pacienteRepository;
    // Repositorio para guardar, buscar y revocar los refresh tokens
    private final RefreshTokenRepository refreshTokenRepository;
    // Servicio de Spring Security que cifra contraseñas con BCrypt antes de guardarlas
    private final PasswordEncoder passwordEncoder;
    // Servicio propio que genera y valida los JWT (JSON Web Tokens) de acceso
    private final JwtService jwtService;
    // Componente de Spring Security que verifica las credenciales del usuario contra la base de datos
    private final AuthenticationManager authenticationManager;
    // Servicio propio que envía correos electrónicos (verificación, reset de contraseña, notificaciones)
    private final EmailService emailService;

    // @Value: inyecta el valor de la propiedad 'app.jwt.refresh-expiration-days' del application.properties;
    // si no está definida, usa 7 como valor por defecto (duración del refresh token en días)
    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    /**
     * Registra el usuario y envía correo de confirmación.
     * Devuelve la URL de verificación si no hay SMTP configurado (modo demo).
     */
    // @Transactional: si cualquier operación dentro del método falla, todas las operaciones de base de datos se revierten
    @Transactional
    public RegisterResult register(RegisterRequest request) {
        // Verifica si ya existe un usuario con el mismo email para evitar duplicados en la base de datos
        if (userRepository.existsByEmail(request.email())) {
            // Lanza excepción personalizada si el email ya está registrado
            throw new DuplicateResourceException("El email '" + request.email() + "' ya está registrado");
        }

        // Busca el rol de paciente; si no existe en la base de datos, lo crea automáticamente
        Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol("ROLE_PACIENTE").build()));

        // Genera un token UUID aleatorio (ej: "550e8400-e29b-41d4-a716-446655440000")
        // Este token se enviará por email y sirve para confirmar que el usuario es dueño de esa dirección
        String verificationToken = UUID.randomUUID().toString();

        // Construye el objeto User con los datos del registro usando el patrón Builder de Lombok
        User user = User.builder()
                .email(request.email())                              // Email del usuario tal como fue ingresado
                .password(passwordEncoder.encode(request.password())) // Cifra la contraseña con BCrypt antes de guardarla (NUNCA se guarda en texto plano)
                .rol(rolPaciente)                                    // Asigna el rol ROLE_PACIENTE por defecto al registrarse
                .estado(true)                                        // La cuenta está activa desde el principio
                .emailVerified(false)                                // El email aún no ha sido verificado (pendiente de confirmación)
                .verificationToken(verificationToken)                // Guarda el token que se enviará por email para verificar la cuenta
                .build();
        // Persiste el usuario en la base de datos y obtiene la entidad con el ID generado
        user = userRepository.save(user);

        // Crea el perfil de usuario con campos vacíos; el usuario los completará desde su panel
        PerfilUsuario perfil = PerfilUsuario.builder()
                .usuario(user)           // Asocia el perfil al usuario recién creado (relación 1 a 1)
                .nombres("")             // Nombres del usuario (vacíos hasta que el usuario los complete)
                .apellidos("")           // Apellidos del usuario (vacíos hasta que el usuario los complete)
                .tipoIdentificacion("")  // Tipo de documento de identidad (cédula, pasaporte, etc.)
                .identificacion("")      // Número del documento de identidad
                .telefono("")            // Número de teléfono de contacto
                .direccion("")           // Dirección de residencia
                .build();
        // Guarda el perfil vacío en la base de datos para que el registro del usuario quede completo
        perfilRepository.save(perfil);

        // Crea el registro de paciente vinculado al usuario; permite acceder a funcionalidades de citas y tratamientos
        pacienteRepository.save(Paciente.builder()
                .usuario(user)  // Asocia el paciente al usuario recién creado
                .estado(true)   // El paciente está activo desde el inicio
                .build());

        // Genera la respuesta de autenticación con los tokens JWT para que el usuario pueda iniciar sesión inmediatamente
        AuthResponse authResponse = buildAuthResponse(user, perfil);

        // Variable para almacenar la URL de verificación en caso de que el email no pueda enviarse (modo demo)
        String demoVerifyUrl = null;
        try {
            // Intenta enviar el correo de verificación al email del usuario con el link de confirmación
            emailService.sendVerificationEmail(request.email(), verificationToken);
        } catch (Exception e) {
            // Si el email falla (sin SMTP configurado, modo demo, error de red, etc.),
            // registra una advertencia y construye la URL localmente para devolverla en la respuesta
            log.warn("Email de verificación no enviado para {}. Devolviendo URL en respuesta (modo demo).", request.email());
            // Construye la URL que el usuario debería visitar para verificar su cuenta
            demoVerifyUrl = emailService.buildVerificationUrl(verificationToken);
        }

        // Retorna el resultado con la respuesta de autenticación y la URL de verificación (null si el email se envió)
        return new RegisterResult(authResponse, demoVerifyUrl);
    }

    // Record inmutable que encapsula los dos datos que devuelve el proceso de registro
    public record RegisterResult(AuthResponse authResponse, String demoVerifyUrl) {}

    // @Transactional: si la actualización del usuario falla, no se guardan cambios parciales
    @Transactional
    public void verifyEmail(String token) {
        // Busca el usuario que tenga exactamente este token de verificación; si no existe, el token es inválido o ya fue usado
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificación inválido o ya utilizado"));
        // Marca el email del usuario como verificado para que pueda acceder a todas las funcionalidades
        user.setEmailVerified(true);
        // Elimina el token de verificación para que no pueda ser reutilizado (seguridad: un solo uso)
        user.setVerificationToken(null);
        // Persiste los cambios en la base de datos
        userRepository.save(user);
    }

    // @Transactional: garantiza que el usuario, perfil y rol se guarden todos o ninguno
    @Transactional
    public AuthResponse registerConRol(RegisterRequest request, String nombreRol) {
        // Verifica que no exista otro usuario con el mismo email antes de registrar
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("El email '" + request.email() + "' ya está registrado");
        }

        // Busca el rol especificado en la base de datos; lanza excepción si no existe
        // (este método se usa para crear administradores, odontólogos u otros roles específicos)
        Rol rol = rolRepository.findByNombreRol(nombreRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol '" + nombreRol + "' no encontrado"));

        // Construye el usuario con el rol recibido como parámetro (no necesariamente ROLE_PACIENTE)
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // Cifra la contraseña con BCrypt por seguridad
                .rol(rol)         // Asigna el rol que se especificó (admin, odontólogo, etc.)
                .estado(true)     // La cuenta queda activa inmediatamente
                .build();
        // Guarda el usuario y obtiene su ID asignado por la base de datos
        user = userRepository.save(user);

        // Crea el perfil vacío asociado al nuevo usuario
        PerfilUsuario perfil = PerfilUsuario.builder()
                .usuario(user)
                .nombres("").apellidos("")             // Campos personales vacíos, el usuario los completará luego
                .tipoIdentificacion("").identificacion("")
                .telefono("").direccion("")
                .build();
        // Persiste el perfil en la base de datos
        perfilRepository.save(perfil);

        // Genera y devuelve los tokens JWT para que el usuario pueda usar la aplicación inmediatamente
        return buildAuthResponse(user, perfil);
    }

    // @Transactional: evita estados inconsistentes al revocar tokens y generar nuevos en la misma operación
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Delega la verificación de credenciales a Spring Security; internamente consulta la base de datos
        // y compara la contraseña ingresada con el hash BCrypt almacenado
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        // Extrae el objeto UserDetails del resultado de la autenticación; contiene email y roles
        UserDetails userDetails = (UserDetails) auth.getPrincipal();

        // Busca la entidad User completa en la base de datos usando el email obtenido de UserDetails
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Busca el perfil personal del usuario (puede ser null si aún no lo ha completado)
        PerfilUsuario perfil = perfilRepository.findById(user.getId()).orElse(null);

        // Revoca todos los refresh tokens anteriores del usuario para evitar sesiones múltiples activas
        // (buena práctica de seguridad: al iniciar sesión, las sesiones anteriores quedan inválidas)
        refreshTokenRepository.revokeAllByUsuarioId(user.getId());
        // Genera nuevos tokens JWT (access token + refresh token) y devuelve la respuesta de autenticación
        return buildAuthResponse(user, perfil);
    }

    // @Transactional: revocar el token viejo y crear uno nuevo debe ocurrir en la misma transacción
    @Transactional
    public AuthResponse refreshToken(String token) {
        // Busca el refresh token en la base de datos; lanza excepción si no existe
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token no encontrado"));

        // Verifica que el token no haya sido revocado (invalidado) y que no haya expirado
        if (rt.getRevoked() || rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Si el token está revocado o expirado, la renovación de sesión no es posible
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        // Marca el refresh token actual como revocado (patrón de rotación: cada renovación genera un token nuevo)
        rt.setRevoked(true);
        // Persiste el estado revocado del token anterior en la base de datos
        refreshTokenRepository.save(rt);

        // Obtiene el usuario al que pertenecía el refresh token para generar nuevos tokens
        User user = rt.getUsuario();
        // Busca el perfil del usuario para incluirlo en la respuesta
        PerfilUsuario perfil = perfilRepository.findById(user.getId()).orElse(null);
        // Genera nuevos access token y refresh token, y devuelve la respuesta de autenticación
        return buildAuthResponse(user, perfil);
    }

    // @Transactional: el registro o login con OAuth deben completarse como una sola unidad de trabajo
    @Transactional
    public AuthResponse loginOrRegisterOAuth(String email, String provider, String nombre) {
        // Busca el usuario por email; si no existe, lo registra automáticamente (flujo OAuth)
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // El usuario no existe aún — se crea automáticamente al hacer login con Google/GitHub/etc.
            Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                    .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol("ROLE_PACIENTE").build()));
            User newUser = User.builder()
                    .email(email)
                    // Los usuarios OAuth no tienen contraseña propia; se genera un UUID aleatorio como contraseña ficticia
                    // (nunca se usa para autenticar, solo cumple la restricción de campo no nulo en la BD)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .rol(rolPaciente)  // Todo usuario OAuth nuevo es paciente por defecto
                    .estado(true)      // La cuenta queda activa inmediatamente sin verificación de email
                    .provider(provider) // Registra el proveedor OAuth (ej: "google", "github") para referencia futura
                    .build();
            // Persiste el nuevo usuario en la base de datos
            newUser = userRepository.save(newUser);

            // Separa el nombre completo recibido del proveedor OAuth en nombre y apellido
            String[] parts = nombre.split(" ", 2); // Divide por el primer espacio; límite 2 para no perder apellidos compuestos
            // Crea el perfil del usuario con los datos básicos obtenidos del proveedor OAuth
            PerfilUsuario perfil = PerfilUsuario.builder()
                    .usuario(newUser)
                    .nombres(parts[0])                            // Primera palabra del nombre completo
                    .apellidos(parts.length > 1 ? parts[1] : "") // Todo lo que sigue al primer espacio (apellidos)
                    .tipoIdentificacion("").identificacion("")     // Campos que el usuario completará después
                    .telefono("").direccion("")
                    .build();
            // Guarda el perfil en la base de datos
            perfilRepository.save(perfil);

            // Crea el registro de paciente para el nuevo usuario OAuth
            pacienteRepository.save(Paciente.builder().usuario(newUser).estado(true).build());
            return newUser;
        });

        // Obtiene el perfil del usuario (ya sea recién creado o existente)
        PerfilUsuario perfil = perfilRepository.findById(user.getId()).orElse(null);
        // Revoca sesiones anteriores del usuario para mantener una sola sesión activa
        refreshTokenRepository.revokeAllByUsuarioId(user.getId());
        // Genera y devuelve los tokens JWT para el usuario OAuth
        return buildAuthResponse(user, perfil);
    }

    /**
     * Genera token de reset. Devuelve la URL de reset si el email no pudo enviarse
     * (modo demo / sin SMTP configurado), o null si el email se envió correctamente.
     */
    // @Transactional: el token de reset se guarda en la BD; si el guardado falla, no se devuelve URL inválida
    @Transactional
    public String forgotPassword(String email) {
        // Busca el usuario por email; si no existe, lanza excepción para no revelar qué emails están registrados
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una cuenta con ese correo"));

        // Genera un token UUID aleatorio y único que servirá como enlace seguro de un solo uso para restablecer la contraseña
        String token = UUID.randomUUID().toString();
        // Guarda el token de reset en el usuario para poder verificarlo cuando llegue en el enlace del email
        user.setResetToken(token);
        // El token de reset expira en 1 hora; después de ese tiempo el enlace ya no será válido
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        // Persiste el token y su fecha de expiración en la base de datos
        userRepository.save(user);

        try {
            // Intenta enviar el email con el enlace de restablecimiento de contraseña
            emailService.sendPasswordResetEmail(email, token);
            return null; // email enviado exitosamente — no se expone la URL en la respuesta por seguridad
        } catch (Exception e) {
            // Si el email falla (sin SMTP, error de red, etc.), registra el error con detalles
            log.error("[AuthService] Error enviando email de reset a {}: {} — {}", email, e.getClass().getSimpleName(), e.getMessage());
            // Devuelve la URL de reset directamente en la respuesta (solo para entornos de desarrollo/demo)
            return emailService.buildResetUrl(token);
        }
    }

    // @Transactional: el cambio de contraseña y la limpieza del token deben ocurrir juntos
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Busca el usuario que tenga exactamente este token de reset; si no existe, el token es inválido o ya fue usado
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token inválido o ya utilizado"));

        // Verifica que el token no haya expirado; si expiró, el usuario debe solicitar otro enlace
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de recuperación ha expirado. Solicita uno nuevo.");
        }

        // Cifra la nueva contraseña con BCrypt antes de guardarla (NUNCA se guarda en texto plano)
        user.setPassword(passwordEncoder.encode(newPassword));
        // Elimina el token de reset para que el enlace ya no pueda ser reutilizado (seguridad: un solo uso)
        user.setResetToken(null);
        // Limpia la fecha de expiración ya que el token fue consumido
        user.setResetTokenExpiry(null);
        // Persiste los cambios (nueva contraseña cifrada, token eliminado) en la base de datos
        userRepository.save(user);
    }

    // Método privado reutilizable que construye la respuesta de autenticación completa con tokens JWT
    private AuthResponse buildAuthResponse(User user, PerfilUsuario perfil) {
        // Construye el objeto UserDetails estándar de Spring Security que JwtService necesita para generar el token
        // Contiene: email (como username), contraseña cifrada y el rol del usuario
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())               // El email se usa como identificador único en el JWT
                .password(user.getPassword())                // Contraseña cifrada (requerida por la interfaz, no se incluye en el JWT)
                .authorities(user.getRol().getNombreRol())   // El rol se incluye en el JWT para autorización en cada request
                .build();

        // Genera el JWT de acceso (access token); contiene email, rol y fecha de expiración
        // Este token se usa en cada request para identificar y autorizar al usuario (válido por pocos minutos/horas)
        String accessToken = jwtService.generateToken(ud);

        // Construye el refresh token: token de larga duración (días) que permite renovar el access token sin reloguear
        // El ciclo de vida es: access token expira → cliente envía refresh token → servidor emite nuevo access token
        RefreshToken rt = RefreshToken.builder()
                .usuario(user)                                            // Asocia el refresh token al usuario propietario
                .token(UUID.randomUUID().toString())                      // UUID aleatorio único como identificador seguro del refresh token
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays)) // Fecha de expiración según configuración (por defecto 7 días)
                .revoked(false)                                           // El token comienza como activo (no revocado)
                .build();
        // Persiste el refresh token en la base de datos para poder validarlo en futuras solicitudes de renovación
        refreshTokenRepository.save(rt);

        // Construye el DTO con los datos públicos del usuario que se devuelven en la respuesta (sin contraseña)
        UserDto userDto = new UserDto(
                user.getId(),                                         // ID interno del usuario en la base de datos
                user.getEmail(),                                      // Email del usuario para identificación en el frontend
                user.getRol().getNombreRol(),                         // Rol (ROLE_PACIENTE, ROLE_ADMIN, etc.) para control de acceso en el frontend
                user.getEstado(),                                     // Estado activo/inactivo de la cuenta
                perfil != null ? perfil.getNombres() : null,          // Nombres personales si el perfil ya fue completado
                perfil != null ? perfil.getApellidos() : null         // Apellidos si el perfil ya fue completado
        );

        // Devuelve la respuesta completa con access token (corto plazo), refresh token (largo plazo) y datos del usuario
        return new AuthResponse(accessToken, rt.getToken(), userDto);
    }
}

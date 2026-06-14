package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.auth.AuthResponse;
import co.edu.sena.productsreact.dto.auth.LoginRequest;
import co.edu.sena.productsreact.dto.auth.RegisterRequest;
import co.edu.sena.productsreact.dto.auth.UserDto;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.RefreshToken;
import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
import co.edu.sena.productsreact.repository.RefreshTokenRepository;
import co.edu.sena.productsreact.repository.RolRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import co.edu.sena.productsreact.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final PacienteRepository pacienteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("El email '" + request.email() + "' ya está registrado");
        }

        Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol("ROLE_PACIENTE").build()));

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .rol(rolPaciente)
                .estado(true)
                .build();
        user = userRepository.save(user);

        PerfilUsuario perfil = PerfilUsuario.builder()
                .usuario(user)
                .nombres("")
                .apellidos("")
                .tipoIdentificacion("")
                .identificacion("")
                .telefono("")
                .direccion("")
                .build();
        perfilRepository.save(perfil);

        pacienteRepository.save(Paciente.builder()
                .usuario(user)
                .estado(true)
                .build());

        return buildAuthResponse(user, perfil);
    }

    @Transactional
    public AuthResponse registerConRol(RegisterRequest request, String nombreRol) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("El email '" + request.email() + "' ya está registrado");
        }

        Rol rol = rolRepository.findByNombreRol(nombreRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol '" + nombreRol + "' no encontrado"));

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .rol(rol)
                .estado(true)
                .build();
        user = userRepository.save(user);

        PerfilUsuario perfil = PerfilUsuario.builder()
                .usuario(user)
                .nombres("").apellidos("")
                .tipoIdentificacion("").identificacion("")
                .telefono("").direccion("")
                .build();
        perfilRepository.save(perfil);

        return buildAuthResponse(user, perfil);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        PerfilUsuario perfil = perfilRepository.findById(user.getId()).orElse(null);

        refreshTokenRepository.revokeAllByUsuarioId(user.getId());
        return buildAuthResponse(user, perfil);
    }

    @Transactional
    public AuthResponse refreshToken(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token no encontrado"));

        if (rt.getRevoked() || rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        User user = rt.getUsuario();
        PerfilUsuario perfil = perfilRepository.findById(user.getId()).orElse(null);
        return buildAuthResponse(user, perfil);
    }

    @Transactional
    public AuthResponse loginOrRegisterOAuth(String email, String provider, String nombre) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                    .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol("ROLE_PACIENTE").build()));
            User newUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .rol(rolPaciente)
                    .estado(true)
                    .provider(provider)
                    .build();
            newUser = userRepository.save(newUser);

            String[] parts = nombre.split(" ", 2);
            PerfilUsuario perfil = PerfilUsuario.builder()
                    .usuario(newUser)
                    .nombres(parts[0])
                    .apellidos(parts.length > 1 ? parts[1] : "")
                    .tipoIdentificacion("").identificacion("")
                    .telefono("").direccion("")
                    .build();
            perfilRepository.save(perfil);

            pacienteRepository.save(Paciente.builder().usuario(newUser).estado(true).build());
            return newUser;
        });

        PerfilUsuario perfil = perfilRepository.findById(user.getId()).orElse(null);
        refreshTokenRepository.revokeAllByUsuarioId(user.getId());
        return buildAuthResponse(user, perfil);
    }

    /**
     * Genera token de reset. Devuelve la URL de reset si el email no pudo enviarse
     * (modo demo / sin SMTP configurado), o null si el email se envió correctamente.
     */
    @Transactional
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una cuenta con ese correo"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(email, token);
            return null; // email enviado — no exponer URL en respuesta
        } catch (Exception e) {
            log.warn("Email no enviado para {}. Devolviendo URL de reset en respuesta (modo demo).", email);
            return emailService.buildResetUrl(token);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token inválido o ya utilizado"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de recuperación ha expirado. Solicita uno nuevo.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, PerfilUsuario perfil) {
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRol().getNombreRol())
                .build();

        String accessToken = jwtService.generateToken(ud);

        RefreshToken rt = RefreshToken.builder()
                .usuario(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays))
                .revoked(false)
                .build();
        refreshTokenRepository.save(rt);

        UserDto userDto = new UserDto(
                user.getId(),
                user.getEmail(),
                user.getRol().getNombreRol(),
                user.getEstado(),
                perfil != null ? perfil.getNombres() : null,
                perfil != null ? perfil.getApellidos() : null
        );

        return new AuthResponse(accessToken, rt.getToken(), userDto);
    }
}

package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.auth.AuthResponse;
import co.edu.sena.productsreact.dto.auth.LoginRequest;
import co.edu.sena.productsreact.dto.auth.RegisterRequest;
import co.edu.sena.productsreact.dto.auth.UserDto;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.RefreshToken;
import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
import co.edu.sena.productsreact.repository.RefreshTokenRepository;
import co.edu.sena.productsreact.repository.RolRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import co.edu.sena.productsreact.security.JwtService;
import lombok.RequiredArgsConstructor;
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
public class AuthService {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("El email '" + request.email() + "' ya está registrado");
        }

        Rol rolPaciente = rolRepository.findByNombreRol("ROLE_PACIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Rol ROLE_PACIENTE no encontrado"));

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
                user.getEstado()
        );

        return new AuthResponse(accessToken, rt.getToken(), userDto);
    }
}

package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.perfil.CambiarPasswordRequest;
import co.edu.sena.productsreact.dto.perfil.PerfilRequest;
import co.edu.sena.productsreact.dto.perfil.PerfilResponse;
import co.edu.sena.productsreact.entity.Paciente;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.PacienteRepository;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerfilService {

    private final UserRepository userRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PerfilResponse getMiPerfil(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        PerfilUsuario perfil = perfilRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        boolean esPaciente = "ROLE_PACIENTE".equals(user.getRol().getNombreRol());
        if (esPaciente) {
            Paciente paciente = pacienteRepository.findByUsuarioId(user.getId()).orElse(null);
            return PerfilResponse.from(user, perfil, paciente);
        }
        return PerfilResponse.from(user, perfil);
    }

    @Transactional
    public PerfilResponse actualizarPerfil(String email, PerfilRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        PerfilUsuario perfil = perfilRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        if (req.nombres() != null) perfil.setNombres(req.nombres());
        if (req.apellidos() != null) perfil.setApellidos(req.apellidos());
        if (req.tipoIdentificacion() != null) perfil.setTipoIdentificacion(req.tipoIdentificacion());
        if (req.identificacion() != null) perfil.setIdentificacion(req.identificacion());
        if (req.telefono() != null) perfil.setTelefono(req.telefono());
        if (req.direccion() != null) perfil.setDireccion(req.direccion());
        if (req.fotoPerfil() != null) perfil.setFotoPerfil(req.fotoPerfil());
        if (req.genero() != null) perfil.setGenero(req.genero());
        perfilRepository.save(perfil);

        boolean esPaciente = "ROLE_PACIENTE".equals(user.getRol().getNombreRol());
        if (esPaciente && req.fechaNacimiento() != null) {
            Paciente paciente = pacienteRepository.findByUsuarioId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Registro de paciente no encontrado"));
            paciente.setFechaNacimiento(req.fechaNacimiento());
            pacienteRepository.save(paciente);
            return PerfilResponse.from(user, perfil, paciente);
        }

        return PerfilResponse.from(user, perfil);
    }

    @Transactional
    public void cambiarPassword(String email, CambiarPasswordRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!passwordEncoder.matches(req.passwordActual(), user.getPassword())) {
            throw new IllegalArgumentException("Contraseña actual incorrecta");
        }
        user.setPassword(passwordEncoder.encode(req.passwordNuevo()));
        userRepository.save(user);
        emailService.sendPasswordChangedEmail(email);
    }
}

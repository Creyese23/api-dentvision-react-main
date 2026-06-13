package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.usuario.CreateUsuarioRequest;
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;
import co.edu.sena.productsreact.entity.PerfilUsuario;
import co.edu.sena.productsreact.entity.Rol;
import co.edu.sena.productsreact.entity.User;
import co.edu.sena.productsreact.exception.DuplicateResourceException;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.PerfilUsuarioRepository;
import co.edu.sena.productsreact.repository.RolRepository;
import co.edu.sena.productsreact.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PerfilUsuarioRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponse> listar() {
        return userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEstado()))
                .map(UsuarioResponse::from).toList();
    }

    public List<UsuarioResponse> listarTodos() {
        return userRepository.findAll().stream()
                .map(UsuarioResponse::from).toList();
    }

    @Transactional
    public UsuarioResponse toggleEstado(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " no encontrado"));
        user.setEstado(!Boolean.TRUE.equals(user.getEstado()));
        return UsuarioResponse.from(userRepository.save(user));
    }

    @Transactional
    public UsuarioResponse crear(CreateUsuarioRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("El email '" + req.email() + "' ya está registrado");
        }
        Rol rol = rolRepository.findByNombreRol(req.rol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol '" + req.rol() + "' no encontrado"));

        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .rol(rol)
                .estado(true)
                .build();
        user = userRepository.save(user);

        perfilRepository.save(PerfilUsuario.builder()
                .usuario(user)
                .nombres("").apellidos("")
                .tipoIdentificacion("").identificacion("")
                .telefono("").direccion("")
                .build());

        return UsuarioResponse.from(user);
    }
}

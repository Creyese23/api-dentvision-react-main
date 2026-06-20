package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.usuario.CreateUsuarioRequest;
import co.edu.sena.productsreact.dto.usuario.UsuarioResponse;
import co.edu.sena.productsreact.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UsuarioResponse> toggleEstado(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.toggleEstado(id));
    }

    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, body.get("rol")));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CreateUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }
}

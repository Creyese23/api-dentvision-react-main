package co.edu.sena.productsreact.controller;

import co.edu.sena.productsreact.dto.perfil.CambiarPasswordRequest;
import co.edu.sena.productsreact.dto.perfil.PerfilRequest;
import co.edu.sena.productsreact.dto.perfil.PerfilResponse;
import co.edu.sena.productsreact.service.PerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> getMiPerfil(Authentication auth) {
        return ResponseEntity.ok(perfilService.getMiPerfil(auth.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<PerfilResponse> actualizarPerfil(Authentication auth,
                                                            @RequestBody PerfilRequest req) {
        return ResponseEntity.ok(perfilService.actualizarPerfil(auth.getName(), req));
    }

    @PostMapping("/me/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(Authentication auth,
                                                 @RequestBody CambiarPasswordRequest req) {
        perfilService.cambiarPassword(auth.getName(), req);
        return ResponseEntity.noContent().build();
    }
}

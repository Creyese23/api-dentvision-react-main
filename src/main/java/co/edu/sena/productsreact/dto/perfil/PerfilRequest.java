package co.edu.sena.productsreact.dto.perfil;

import java.time.LocalDate;

public record PerfilRequest(
        String nombres,
        String apellidos,
        String tipoIdentificacion,
        String identificacion,
        String telefono,
        String direccion,
        String fotoPerfil,
        LocalDate fechaNacimiento,
        String genero
) {}

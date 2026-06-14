package co.edu.sena.productsreact.dto.perfil;

public record PerfilRequest(
        String nombres,
        String apellidos,
        String tipoIdentificacion,
        String identificacion,
        String telefono,
        String direccion,
        String fotoPerfil
) {}

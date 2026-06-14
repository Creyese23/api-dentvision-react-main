package co.edu.sena.productsreact.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfiles_usuario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerfilUsuario {

    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private User usuario;

    @Column(length = 100, columnDefinition = "VARCHAR(100) NULL DEFAULT ''")
    private String nombres;

    @Column(length = 100, columnDefinition = "VARCHAR(100) NULL DEFAULT ''")
    private String apellidos;

    @Column(name = "tipo_identificacion", length = 30, columnDefinition = "VARCHAR(30) NULL DEFAULT ''")
    private String tipoIdentificacion;

    @Column(length = 20, columnDefinition = "VARCHAR(20) NULL DEFAULT ''")
    private String identificacion;

    @Column(length = 20, columnDefinition = "VARCHAR(20) NULL DEFAULT ''")
    private String telefono;

    @Column(length = 200, columnDefinition = "VARCHAR(200) NULL DEFAULT ''")
    private String direccion;

    @Column(name = "foto_perfil", columnDefinition = "LONGTEXT")
    private String fotoPerfil;
}

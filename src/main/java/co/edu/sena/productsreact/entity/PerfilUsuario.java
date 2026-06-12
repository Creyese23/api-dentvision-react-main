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

    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @Column(name = "tipo_identificacion", length = 30)
    private String tipoIdentificacion;

    @Column(length = 20)
    private String identificacion;

    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;
}

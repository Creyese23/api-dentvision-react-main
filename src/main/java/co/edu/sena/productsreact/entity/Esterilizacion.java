package co.edu.sena.productsreact.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Estado vigente de esterilización del instrumental requerido para una cita. */
@Entity
@Table(name = "esterilizaciones", uniqueConstraints = @UniqueConstraint(columnNames = "id_cita"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Esterilizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Una cita tiene un único estado vigente de esterilización. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", nullable = false, unique = true)
    private Cita cita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoEsterilizacion estado = EstadoEsterilizacion.NO_DISPONIBLE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    /** Usuario autenticado que hizo la última actualización. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_responsable", nullable = false)
    private User responsable;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad Atencion - Registro de una atención brindada a una solicitud.
 */
@Entity
@Table(name = "atencion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Atencion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idestadoatencion", nullable = false)
    private EstadoAtencion estadoAtencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcaso", nullable = false)
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtiposervicio")
    private TipoServicio tipoServicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idlugarentrevista")
    private LugarEntrevista lugarEntrevista;

    @Column(name = "logroacuerdo", nullable = false)
    private boolean logroAcuerdo;

    @Column(name = "fechacreacion")
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuariocreacion")
    private Usuario usuarioCreacion;

    @Column(name = "fechaactualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuarioactualizacion")
    private Usuario usuarioActualizacion;

    @OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<ArchivoConsentimiento> archivos = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}

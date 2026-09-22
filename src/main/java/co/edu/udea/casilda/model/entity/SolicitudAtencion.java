package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad SolicitudAtencion - Solicitud de atención registrada en el sistema.
 * Relaciona un caso con la solicitud de servicios.
 */
@Entity
@Table(name = "solicitudatencion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAtencion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idremision")
    private Remision remision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idsolicitante", nullable = false)
    private Persona solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ididentidadgenero")
    private IdentidadGenero identidadGenero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuarioactualizacion")
    private Usuario usuarioActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtiposolicitud", nullable = false)
    private TipoSolicitud tipoSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idestadosolicitud", nullable = false)
    private EstadoSolicitud estadoSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idmediosolicitud")
    private MedioSolicitud medioSolicitud;

    @Column(name = "fechacreacion")
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuariocreacion")
    private Usuario usuarioCreacion;

    @Column(name = "fechaactualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "observacionestelefono", length = 500, nullable = true)
    private String observacionesTelefono;

    @Column(name = "observacionescorreo", length = 500, nullable = true)
    private String observacionesCorreo;

    @OneToMany(mappedBy = "solicitudAtencion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContactoTelefonico> contactosTelefonicos = new ArrayList<>();

    @OneToMany(mappedBy = "solicitudAtencion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Asignacion> asignaciones = new ArrayList<>();

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

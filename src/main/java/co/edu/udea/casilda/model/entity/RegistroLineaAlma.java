package co.edu.udea.casilda.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registrolinealma")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroLineaAlma extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idpersona")
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtiporeporte", nullable = false)
    private TipoReporteAlma tipoReporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcanalcontacto", nullable = false)
    private CanalContacto canalContacto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idquienremite")
    private ActorRemitente quienRemite;

    @Column(name = "fechahoraatencion", nullable = false)
    private LocalDateTime fechaHoraAtencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idpersonaatiende", nullable = false)
    private GrupoProfesional personaAtiende;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtiposervicio", nullable = false)
    private TipoServicio tipoServicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idpersonaregistra", nullable = false)
    private Usuario personaRegistra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idlugarentrevista")
    private LugarEntrevista lugarEntrevista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ididentidadgenero")
    private IdentidadGenero identidadGenero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idorientacionsexual")
    private OrientacionSexual orientacionSexual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idetnia")
    private Etnia etnia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idciudadresidencia")
    private Municipio ciudadResidencia;

    @Column(name = "direccionresidencia")
    private String direccionResidencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idvinculoudea")
    private VinculoUdeA vinculoUdeA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idunidadacademica")
    private UnidadAcademica unidadAcademica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idprograma")
    private Programa programa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idunidadadministrativa")
    private UnidadAdministrativa unidadAdministrativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcampus")
    private Campus campus;

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

    @OneToOne(mappedBy = "registroLineaAlma", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AtencionAph atencionAph;

    @OneToMany(mappedBy = "registroLineaAlma", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RemisionRegistroAlma> remisiones = new ArrayList<>();

    @OneToMany(mappedBy = "registroLineaAlma", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContactoLineaAlma> contactos = new ArrayList<>();

    @Column(name = "observacionescorreo", length = 500, nullable = true)
    private String observacionesCorreo;

    @Column(name = "observacionestelefono", length = 500, nullable = true)
    private String observacionesTelefono;

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

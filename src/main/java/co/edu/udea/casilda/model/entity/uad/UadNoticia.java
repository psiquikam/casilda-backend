package co.edu.udea.casilda.model.entity.uad;

import co.edu.udea.casilda.model.entity.Auditable;
import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.model.enums.uad.ActorRuta;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoOrigenNoticia;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Noticia disciplinaria NDI o NDA (UAD-ID-01/02, UAD-NOT).
 *
 * <p>Una noticia <strong>no</strong> es un caso: el {@code casoId} CASILDA solo se asigna o vincula con
 * la decisión «Continuar trámite» (UAD-EVA-03). El consecutivo lo asigna {@code UadConsecutivoService}
 * con secuencias separadas por tipo y año.</p>
 *
 * <p>Extiende {@code Auditable}. Una NDA se radica en un contexto anónimo ({@code UadSinIdentidad}), así que
 * su {@code created_by} queda en {@code ANONYMOUS} aunque llegue un token (UAD-NOT-04, UAD-SEG-06).</p>
 */
@Entity
@Table(name = "uad_noticia",
        uniqueConstraints = @UniqueConstraint(name = "uk_uad_noticia_consecutivo", columnNames = "consecutivo"),
        indexes = {
                @Index(name = "ix_uad_noticia_tipo_fecha", columnList = "tipo, fecha_recepcion"),
                @Index(name = "ix_uad_noticia_reporte", columnList = "idreportecaso")
        })
@Getter
@Setter
@NoArgsConstructor
public class UadNoticia extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "consecutivo", nullable = false, length = 20)
    private String consecutivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 3)
    private TipoNoticia tipo;

    /** ID de caso CASILDA: nulo hasta «Continuar trámite», salvo que venga de otro actor (UAD-NOT-03). */
    @Column(name = "caso_id", length = 40)
    private String casoId;

    @Column(name = "expediente_uad", length = 40)
    private String expedienteUad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iduadcaso")
    private UadCaso casoUad;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_tipo", nullable = false, length = 30)
    private TipoOrigenNoticia origenTipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_remitente", length = 20)
    private ActorRuta actorRemitente;

    @Column(name = "fecha_recepcion", nullable = false, updatable = false)
    private Instant fechaRecepcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoNoticia estado;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "vinculacion_declarada", length = 25)
    private VinculacionInvestigado vinculacionDeclarada;

    @Enumerated(EnumType.STRING)
    @Column(name = "vinculacion_clasificada", length = 25)
    private VinculacionInvestigado vinculacionClasificada;

    /** Reporte de origen de una NDI radicada desde el formulario general. Nulo en NDA. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idreportecaso")
    private ReporteCaso reporteCaso;

    @Column(name = "descripcion_hechos", nullable = false, columnDefinition = "text")
    private String descripcionHechos;

    @Column(name = "lugar_ocurrencia", length = 300)
    private String lugarOcurrencia;

    @Column(name = "presunto_responsable", length = 300)
    private String presuntoResponsable;

    /** Vinculación vigente: la clasificada por la UAD o, si no hay, la declarada al radicar. */
    public VinculacionInvestigado vinculacionEfectiva() {
        return vinculacionClasificada != null ? vinculacionClasificada : vinculacionDeclarada;
    }
}

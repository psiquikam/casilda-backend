package co.edu.udea.casilda.model.entity.uad;

import co.edu.udea.casilda.model.entity.Auditable;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import co.edu.udea.casilda.model.enums.uad.TipoEventoUad;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Evento del historial UAD (UAD-NOT-12, UAD-SEG-03, UAD-HIS-02). <strong>Append-only</strong>:
 * {@code @Immutable} impide que Hibernate emita UPDATE; ningún servicio borra eventos. La rectificación
 * auditada futura agrega un evento {@code RECTIFICACION}, nunca reescribe el original (D-UAD-06).
 */
@Entity
@Immutable
@Table(name = "uad_evento", indexes = {
        @Index(name = "ix_uad_evento_noticia", columnList = "iduadnoticia, fecha"),
        @Index(name = "ix_uad_evento_caso", columnList = "iduadcaso, fecha")
})
@Getter
@Setter
@NoArgsConstructor
public class UadEvento extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iduadnoticia")
    private UadNoticia noticia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iduadcaso")
    private UadCaso casoUad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoEventoUad tipo;

    @Column(name = "fecha", nullable = false, updatable = false)
    private Instant fecha;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    /** Nombre del profesional. Nulo en eventos sin autor (p. ej. radicación de una NDA). */
    @Column(name = "profesional", length = 200)
    private String profesional;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipo", length = 10)
    private EquipoUad equipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase", length = 12)
    private FaseUad fase;

    @Column(name = "estado_anterior", length = 40)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 40)
    private String estadoNuevo;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;
}

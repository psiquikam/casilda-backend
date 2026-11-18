package co.edu.udea.casilda.model.entity.uad;

import co.edu.udea.casilda.model.entity.Auditable;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoGestionUad;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Gestión UAD de un caso admitido (UAD-CAS). Nace con «Continuar trámite»; su {@code casoId} es el ID
 * de caso CASILDA (nuevo o conservado). La gestión UAD tiene estado propio: finalizarla no cierra el
 * caso CASILDA global (UAD-ID-10, UAD-SEG-04).
 *
 * <p>Entrega 1 solo la crea en la admisión. Bandeja, actuaciones y traslado son de la Entrega 2.</p>
 */
@Entity
@Table(name = "uad_caso", indexes = @Index(name = "ix_uad_caso_equipo_fase", columnList = "equipo_responsable, fase_actual, estado_gestion"))
@Getter
@Setter
@NoArgsConstructor
public class UadCaso extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "caso_id", nullable = false, length = 40)
    private String casoId;

    @Column(name = "expediente_uad", length = 40)
    private String expedienteUad;

    @Enumerated(EnumType.STRING)
    @Column(name = "vinculacion_investigado", nullable = false, length = 25)
    private VinculacionInvestigado vinculacionInvestigado;

    @Column(name = "victima_nombre", length = 250)
    private String victimaNombre;

    @Column(name = "fecha_admision", nullable = false, updatable = false)
    private Instant fechaAdmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_actual", nullable = false, length = 12)
    private FaseUad faseActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipo_responsable", nullable = false, length = 10)
    private EquipoUad equipoResponsable;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_gestion", nullable = false, length = 12)
    private EstadoGestionUad estadoGestion;

    @Column(name = "motivo_fin_gestion", length = 30)
    private String motivoFinGestion;

    @Column(name = "fecha_ultimo_traslado")
    private Instant fechaUltimoTraslado;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

package co.edu.udea.casilda.model.entity.uad;

import co.edu.udea.casilda.model.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Solicitud de información adicional al reportante identificado (UAD-EVA-04). El vencimiento es
 * informativo: <strong>no</strong> cierra la noticia (UAD-EVA-05). El reportante responde desde «Mis reportes»
 * ({@code UadMisReportesService}); por ahora solo con texto (política de evidencias pendiente, D-UAD-09).
 */
@Entity
@Table(name = "uad_solicitud_informacion")
@Getter
@Setter
@NoArgsConstructor
public class UadSolicitudInformacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iduadnoticia", nullable = false)
    private UadNoticia noticia;

    @Column(name = "tipo_solicitud_codigo", nullable = false, length = 50)
    private String tipoSolicitudCodigo;

    @Column(name = "informacion_requerida", nullable = false, length = 2000)
    private String informacionRequerida;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private Instant fechaSolicitud;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Column(name = "respondida", nullable = false)
    private boolean respondida;

    /** Texto con el que respondió el reportante. Nulo mientras no responda. */
    @Column(name = "respuesta", columnDefinition = "text")
    private String respuesta;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta;
}

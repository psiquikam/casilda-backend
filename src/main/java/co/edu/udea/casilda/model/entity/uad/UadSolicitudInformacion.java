package co.edu.udea.casilda.model.entity.uad;

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
 * informativo: <strong>no</strong> cierra la noticia (UAD-EVA-05). La respuesta desde «Mis reportes»
 * es de la Entrega 3 (bloqueada por D-UAD-16).
 */
@Entity
@Table(name = "uad_solicitud_informacion")
@Getter
@Setter
@NoArgsConstructor
public class UadSolicitudInformacion {

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
}

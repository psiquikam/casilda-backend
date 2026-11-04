package co.edu.udea.casilda.model.entity;

import co.edu.udea.casilda.model.enums.EstadoReporteRutaEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Reporte inicial de un caso enviado desde el formulario anónimo o autenticado.
 *
 * <p>Se mantiene separado de {@link Caso} porque el reporte puede recibirse sin
 * identidad, cita o información completa de atención.</p>
 */
@Entity
@Table(name = "reportecaso", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reportecaso_codigo", columnNames = "codigo")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteCaso extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipoidentificacion")
    private TipoIdentificacion tipoDocumento;

    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "tipo_lugar", length = 8)
    private String tipoLugar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcampus")
    private Campus campus;

    @Column(name = "facultad", length = 120)
    private String facultad;

    @Column(name = "bloque", length = 60)
    private String bloque;

    @Column(name = "espacio", length = 120)
    private String espacio;

    @Column(name = "lugar_detalle_externo", length = 255)
    private String lugarDetalleExterno;

    @Column(name = "descripcion_hechos", nullable = false, columnDefinition = "text")
    private String descripcionHechos;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idrutaactivacion", nullable = false)
    private RutaActivacion rutaActivacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_ruta", nullable = false, length = 10)
    private EstadoReporteRutaEnum estadoRuta = EstadoReporteRutaEnum.SOLICITADA;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = Instant.now();
        }
        if (estadoRuta == null) {
            estadoRuta = EstadoReporteRutaEnum.SOLICITADA;
        }
    }
}

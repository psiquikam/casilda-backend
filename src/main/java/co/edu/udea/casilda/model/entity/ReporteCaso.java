package co.edu.udea.casilda.model.entity;

import co.edu.udea.casilda.model.enums.EstadoReporteRutaEnum;
import co.edu.udea.casilda.model.enums.TipoReportanteEnum;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reportante", length = 8)
    private TipoReportanteEnum tipoReportante;

    @Column(name = "victima_primer_nombre", length = 60)
    private String victimaPrimerNombre;

    @Column(name = "victima_segundo_nombre", length = 60)
    private String victimaSegundoNombre;

    @Column(name = "victima_primer_apellido", length = 60)
    private String victimaPrimerApellido;

    @Column(name = "victima_segundo_apellido", length = 60)
    private String victimaSegundoApellido;

    @Column(name = "victima_correo", length = 254)
    private String victimaCorreo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idvictima_identidad_genero")
    private IdentidadGenero victimaIdentidadGenero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idvictima_cargo")
    private Cargo victimaCargo;

    @Column(name = "desea_contacto")
    private Boolean deseaContacto;

    @Column(name = "contacto_correo", length = 254)
    private String contactoCorreo;

    @Column(name = "contacto_whatsapp", length = 12)
    private String contactoWhatsapp;

    @Column(name = "agresor_numero_documento", length = 20)
    private String agresorNumeroDocumento;

    @Column(name = "agresor_primer_nombre", length = 60)
    private String agresorPrimerNombre;

    @Column(name = "agresor_segundo_nombre", length = 60)
    private String agresorSegundoNombre;

    @Column(name = "agresor_primer_apellido", length = 60)
    private String agresorPrimerApellido;

    @Column(name = "agresor_segundo_apellido", length = 60)
    private String agresorSegundoApellido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idagresor_vinculo_udea")
    private VinculoUdeA agresorVinculoUdeA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idagresor_vinculo_victima")
    private VinculoAgresorVictima agresorVinculoVictima;

    @Column(name = "agresor_otro_vinculo_udea", length = 255)
    private String agresorOtroVinculoUdeA;

    @Column(name = "agresor_otro_vinculo_victima", length = 255)
    private String agresorOtroVinculoVictima;

    @Column(name = "agresor_informacion_adicional", length = 1000)
    private String agresorInformacionAdicional;

    @Column(name = "agresor_remitir_uad")
    private Boolean agresorRemitirUad;

    @Column(name = "agresor_remitir_uad_fecha")
    private Instant agresorRemitirUadFecha;

    @Column(name = "agresor_remitir_uad_version", length = 30)
    private String agresorRemitirUadVersion;

    @OneToMany(mappedBy = "reporteCaso", fetch = FetchType.LAZY, cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<EvidenciaReporte> evidencias = new ArrayList<>();

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

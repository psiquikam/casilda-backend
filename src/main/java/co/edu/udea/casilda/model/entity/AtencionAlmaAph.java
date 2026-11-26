package co.edu.udea.casilda.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "atencionalmaaph")
@Getter
@Setter
@NoArgsConstructor
public class AtencionAlmaAph {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16, updatable = false)
    private String codigo;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(length = 10)
    private String triaje;

    @Column(name = "justificacionomisiontriaje", length = 1000)
    private String justificacionOmisionTriaje;

    @Column(name = "fechaderivacion")
    private Instant fechaDerivacion;

    @Column(name = "consentimientomedio", length = 10)
    private String consentimientoMedio;

    @Column(name = "consentimientofecha")
    private Instant consentimientoFecha;

    @Column(name = "consentimientoversion", length = 80)
    private String consentimientoVersion;

    @Column(name = "consentimientorecibido")
    private Instant consentimientoRecibido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuariotomador")
    private Usuario tomadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuariotomadorpsicologia")
    private Usuario tomadaPorPsicologia;

    @Column(name = "fechatomapsicologia")
    private Instant fechaTomaPsicologia;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode datos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datosremision", columnDefinition = "jsonb")
    private JsonNode datosRemision;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(nullable = false)
    private Instant fechaActualizacion;

    private Instant fechaNota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idautornota")
    private Usuario autorNota;
}

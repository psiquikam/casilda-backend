package co.edu.udea.casilda.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rutaexternaalma")
@Getter
@Setter
@NoArgsConstructor
public class RutaExternaAlma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idatencion", nullable = false, updatable = false)
    private AtencionAlmaAph atencion;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 80)
    private String entidad;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "fechaactivacion", nullable = false)
    private LocalDate fechaActivacion;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(length = 80)
    private String resultado;

    @Column(name = "peligroinminente", nullable = false)
    private boolean peligroInminente;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode consentimiento;

    @Column(name = "fecharegistro", nullable = false, updatable = false)
    private Instant fechaRegistro;
}

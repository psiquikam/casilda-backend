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
@Table(name = "derivacioninternaalma")
@Getter
@Setter
@NoArgsConstructor
public class DerivacionInternaAlma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idatencion", nullable = false, updatable = false)
    private AtencionAlmaAph atencion;

    @Column(nullable = false, length = 80)
    private String destino;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "peligroinminente", nullable = false)
    private boolean peligroInminente;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode consentimiento;

    @Column(name = "fecharegistro", nullable = false, updatable = false)
    private Instant fechaRegistro;
}

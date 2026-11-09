package co.edu.udea.casilda.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Referencia privada a un archivo de evidencia asociado a un reporte.
 */
@Entity
@Table(name = "evidenciareporte")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvidenciaReporte extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idreportecaso", nullable = false)
    private ReporteCaso reporteCaso;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false, unique = true, length = 100)
    private String nombreAlmacenado;

    @Column(name = "tipo_contenido", nullable = false, length = 100)
    private String tipoContenido;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = Instant.now();
        }
    }
}

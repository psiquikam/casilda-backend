package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "archivoconsentimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchivoConsentimiento extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idatencion", nullable = false)
    private Atencion atencion;

    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "tipocontenido", nullable = false, length = 200)
    private String tipoContenido;

    @Column(name = "nombre", nullable = false, length = 500)
    private String nombre;
}

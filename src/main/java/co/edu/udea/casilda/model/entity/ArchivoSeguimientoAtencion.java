package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad ArchivoSeguimientoAtencion - Almacena archivos asociados a un seguimiento de atención
 */
@Entity
@Table(name = "archivoseguimientoatencion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchivoSeguimientoAtencion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idseguimientoatencion", nullable = false)
    private SeguimientoAtencion seguimientoAtencion;

    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "tipocontenido", nullable = false, length = 200)
    private String tipoContenido;

    @Column(name = "nombre", nullable = false, length = 500)
    private String nombre;
}

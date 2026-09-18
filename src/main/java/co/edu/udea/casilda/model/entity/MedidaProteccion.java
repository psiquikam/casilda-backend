package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "medidaproteccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedidaProteccion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idatencion", nullable = false)
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipomedida", nullable = false)
    private TipoMedida tipoMedida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idsubtipomedida", nullable = false)
    private SubTipoMedida subtipoMedida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idresponsablemedidaproteccion", nullable = false)
    private ResponsableMedidaProteccion responsable;

    @Column(name = "fecharegistro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;
}

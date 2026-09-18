package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "modalidadviolenciacaso")
@IdClass(ModalidadViolenciaCasoId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModalidadViolenciaCaso extends Auditable {
    
    @Id
    @Column(name = "idcaso", nullable = false)
    private Long idcaso;

    @Id
    @Column(name = "idmodalidadviolencia", nullable = false)
    private Integer idmodalidadviolencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcaso", nullable = false, insertable = false, updatable = false)
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idmodalidadviolencia", nullable = false, insertable = false, updatable = false)
    private ModalidadViolencia modalidadViolencia;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "compromisopersonaatendida")
@IdClass(CompromisoPersonaAtendidaId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompromisoPersonaAtendida extends Auditable {

    @Id
    @Column(name = "idatencion", nullable = false)
    private Long idatencion;

    @Id
    @Column(name = "idtipocompromiso", nullable = false)
    private Integer idtipocompromiso;

    @Column(name = "fechacompromiso", nullable = false)
    private LocalDateTime fechacompromiso;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idatencion", nullable = false, insertable = false, updatable = false)
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipocompromiso", nullable = false, insertable = false, updatable = false)
    private TipoCompromiso tipoCompromiso;
}

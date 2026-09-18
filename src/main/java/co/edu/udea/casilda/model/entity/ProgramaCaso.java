package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "programacaso")
@IdClass(ProgramaCasoId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramaCaso extends Auditable {
    
    @Id
    @Column(name = "idcaso", nullable = false)
    private Long idcaso;

    @Id
    @Column(name = "idprograma", nullable = false)
    private Integer idprograma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcaso", nullable = false, insertable = false, updatable = false)
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idprograma", nullable = false, insertable = false, updatable = false)
    private Programa programa;
}

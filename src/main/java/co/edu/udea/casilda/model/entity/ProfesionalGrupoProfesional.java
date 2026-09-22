package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad ProfesionalGrupoProfesional - Relación entre profesionales y grupos profesionales.
 */
@Entity
@Table(name = "profesionalgrupoprofesional")
@IdClass(ProfesionalGrupoProfesionalId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalGrupoProfesional extends Auditable {
    
    @Id
    @Column(name = "idgrupoprofesional", nullable = false)
    private Integer idgrupoprofesional;

    @Id
    @Column(name = "idprofesional", nullable = false)
    private Long idprofesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idgrupoprofesional", nullable = false, insertable = false, updatable = false)
    private GrupoProfesional grupoProfesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idprofesional", nullable = false, insertable = false, updatable = false)
    private Profesional profesional;
}

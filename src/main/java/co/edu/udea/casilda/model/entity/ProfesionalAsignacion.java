package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad ProfesionalAsignacion - Relación entre profesionales y asignaciones.
 */
@Entity
@Table(name = "profesionalasignacion")
@IdClass(ProfesionalAsignacionId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalAsignacion extends Auditable {
    
    @Id
    @Column(name = "idasignacion", nullable = false)
    private Long idasignacion;

    @Id
    @Column(name = "idprofesional", nullable = false)
    private Long idprofesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idasignacion", nullable = false, insertable = false, updatable = false)
    private Asignacion asignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idprofesional", nullable = false, insertable = false, updatable = false)
    private Profesional profesional;
}

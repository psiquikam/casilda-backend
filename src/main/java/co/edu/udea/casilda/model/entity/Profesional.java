package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad Profesional - Profesionales que atienden casos en el sistema.
 */
@Entity
@Table(name = "profesional")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profesional extends Auditable {

    @Id
    @Column(name = "idpersona")
    private Long idPersona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idpersona", insertable = false, updatable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcargo", nullable = false)
    private Cargo cargo;

    public Profesional(Persona persona, Cargo cargo) {
        this.persona = persona;
        this.idPersona = persona.getId();
        this.cargo = cargo;
    }
}

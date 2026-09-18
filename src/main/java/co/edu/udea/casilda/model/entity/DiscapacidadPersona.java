package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "discapacidadpersona")
@IdClass(DiscapacidadPersonaId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscapacidadPersona extends Auditable {
    
    @Id
    @Column(name = "idpersona", nullable = false)
    private Long idpersona;

    @Id
    @Column(name = "idsubtipodiscapacidad", nullable = false)
    private Integer idsubtipodiscapacidad;

    @Column(name = "descripcion")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idpersona", nullable = false, insertable = false, updatable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idsubtipodiscapacidad", nullable = false, insertable = false, updatable = false)
    private SubTipoDiscapacidad subTipoDiscapacidad;
}

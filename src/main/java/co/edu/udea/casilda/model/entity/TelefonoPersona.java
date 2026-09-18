package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "telefonopersona")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelefonoPersona extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idpersona", nullable = false)
    private Long idpersona;

    @Column(name = "idtipo", nullable = false)
    private Integer idtipo;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idpersona", nullable = false, insertable = false, updatable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipo", nullable = false, insertable = false, updatable = false)
    private TipoTelefono tipoTelefono;

    @Column(nullable = false, length = 20)
    private String telefono;
}

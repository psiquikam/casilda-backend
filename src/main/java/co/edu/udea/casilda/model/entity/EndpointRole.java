package co.edu.udea.casilda.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "endpointrole",
        uniqueConstraints = @UniqueConstraint(columnNames = {"idendpoint", "idrol"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndpointRole extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idendpoint", nullable = false)
    private Endpoint endpoint;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "idrol", nullable = false)
    private Rol rol;

    public Rol getRole() {
        return rol;
    }

    public void setRole(final Rol role) {
        this.rol = role;
    }
}

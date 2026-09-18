package co.edu.udea.casilda.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "actividad")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Actividad extends Auditable {

    @Id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "idaccion", nullable = false)
    private Accion accion;

    @Column(nullable = false)
    private String nombre;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "programa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Programa extends Auditable {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "idunidadacademica", nullable = false)
    private Integer idunidadacademica;

    @Column(name = "aplicapregrado", nullable = false)
    private Boolean aplicapregrado;

    @Column(name = "aplicaposgrado", nullable = false)
    private Boolean aplicaposgrado;
}


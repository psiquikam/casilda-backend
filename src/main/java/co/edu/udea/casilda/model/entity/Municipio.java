package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "municipio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Municipio extends Auditable {
    @Id
    private Integer id;

    @Column(unique = true, nullable = false)
    private String codigo;

    @Column(unique = true, nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iddepartamento", nullable = false)
    private Departamento departamento;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad GrupoProfesional - Grupos de profesionales en el sistema.
 */
@Entity
@Table(name = "grupoprofesional")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrupoProfesional extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;
}

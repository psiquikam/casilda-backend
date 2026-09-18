package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "modalidadviolenciasexual")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModalidadViolenciaSexual extends Auditable {
    @Id
    private Integer id;

    @Column(unique = true, nullable = false)
    private String nombre;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cargo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cargo extends Auditable {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String nombre;
}

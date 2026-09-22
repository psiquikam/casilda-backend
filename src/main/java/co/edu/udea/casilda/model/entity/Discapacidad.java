package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "discapacidad")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Discapacidad extends Auditable {
    @Id
    private Integer id;

    @Column(unique = true, nullable = false)
    private String nombre;
}

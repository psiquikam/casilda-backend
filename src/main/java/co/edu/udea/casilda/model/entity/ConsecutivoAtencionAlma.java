package co.edu.udea.casilda.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "consecutivoatencionalma")
@Getter
@Setter
@NoArgsConstructor
public class ConsecutivoAtencionAlma {
    @Id
    private Integer anio;

    @Column(nullable = false)
    private Integer ultimo;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subtipodiscapacidad")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubTipoDiscapacidad extends Auditable {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipo", nullable = false)
    private TipoDiscapacidad tipo;
}

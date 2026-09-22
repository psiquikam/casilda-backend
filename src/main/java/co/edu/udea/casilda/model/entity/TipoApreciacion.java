package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipoapreciacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoApreciacion extends Auditable {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idapreciacion", nullable = false)
    private Apreciacion apreciacion;
}

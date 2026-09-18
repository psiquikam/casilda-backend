package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "remisionatencion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RemisionAtencionId.class)
public class RemisionAtencion extends Auditable {
    @Id
    @Column(name = "idatencion")
    private Long idAtencion;

    @Id
    @Column(name = "idtiporemision")
    private Integer idTipoRemision;

    @Column(name = "cual")
    private String cual;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}

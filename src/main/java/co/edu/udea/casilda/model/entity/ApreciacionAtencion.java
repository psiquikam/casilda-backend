package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "apreciacionatencion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApreciacionAtencion extends Auditable {

    @EmbeddedId
    private ApreciacionAtencionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idAtencion")
    @JoinColumn(name = "idatencion")
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idTipoApreciacion")
    @JoinColumn(name = "idtipoapreciacion")
    private TipoApreciacion tipoApreciacion;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;
}

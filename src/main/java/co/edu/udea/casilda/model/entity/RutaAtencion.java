package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rutaatencion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RutaAtencionId.class)
public class RutaAtencion extends Auditable {
    @Id
    @Column(name = "idatencion")
    private Long idAtencion;

    @Id
    @Column(name = "idtiporutaactivacion")
    private Integer idTipoRutaActivacion;

    @Id
    @Column(name = "idrutaactivacion")
    private Integer idRutaActivacion;
}

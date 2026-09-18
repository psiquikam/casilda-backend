package co.edu.udea.casilda.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "remisionregistroalma")
@IdClass(RemisionRegistroAlmaId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemisionRegistroAlma extends Auditable {

    @Id
    @Column(name = "idregistrolinealma", nullable = false)
    private Long idregistrolinealma;

    @Id
    @Column(name = "idtiporemision", nullable = false)
    private Integer idtiporemision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idregistrolinealma", nullable = false, insertable = false, updatable = false)
    private RegistroLineaAlma registroLineaAlma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtiporemision", nullable = false, insertable = false, updatable = false)
    private TipoRemision tipoRemision;

    @Column(name = "cual")
    private String cual;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}

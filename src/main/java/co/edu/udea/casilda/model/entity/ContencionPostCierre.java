package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contencionpostcierre")
@Getter
@Setter
@NoArgsConstructor
public class ContencionPostCierre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idatencion", nullable = false, updatable = false)
    private AtencionAlmaAph atencion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idcaso", nullable = false, updatable = false)
    private Caso caso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idautor", nullable = false, updatable = false)
    private Usuario autor;

    @Column(nullable = false, length = 2000, updatable = false)
    private String nota;

    @Column(name = "fecharegistro", nullable = false, updatable = false)
    private Instant fechaRegistro;
}

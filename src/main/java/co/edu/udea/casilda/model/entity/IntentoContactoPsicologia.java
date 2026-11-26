package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "intentocontactopsicologia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_intentopsicologia_atencion_numero",
                columnNames = {"idatencionalmaaph", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class IntentoContactoPsicologia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idatencionalmaaph", nullable = false)
    private AtencionAlmaAph atencion;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 30)
    private String protocolo;

    @Column(name = "fechahora", nullable = false)
    private Instant fechaHora;

    @Column(nullable = false, length = 40)
    private String resultado;

    @Column(nullable = false, length = 2000)
    private String nota;

    @Column(length = 2000)
    private String justificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idusuario", nullable = false)
    private Usuario autor;
}

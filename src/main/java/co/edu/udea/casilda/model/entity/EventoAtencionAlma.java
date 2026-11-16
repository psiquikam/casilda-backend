package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "eventoatencionalma")
@Getter
@Setter
@NoArgsConstructor
public class EventoAtencionAlma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idatencion", nullable = false)
    private AtencionAlmaAph atencion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idusuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private String accion;

    @Column(nullable = false)
    private Instant fecha;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad Remision - Remisión de personas a servicios o dependencias.
 */
@Entity
@Table(name = "remision")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Remision extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idremitente", nullable = false)
    private Persona remitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcargo", nullable = false)
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idunidadadministrativa", nullable = false)
    private UnidadAdministrativa unidadAdministrativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idunidadacademica", nullable = false)
    private UnidadAcademica unidadAcademica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcampus", nullable = false)
    private Campus campus;

    @Column(name = "fechacreacion")
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuariocreacion")
    private Usuario usuarioCreacion;

    @Column(name = "fechaactualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuarioactualizacion")
    private Usuario usuarioActualizacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}

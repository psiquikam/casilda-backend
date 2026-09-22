package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad Asignacion - Asignación de un grupo profesional a una solicitud de atención.
 */
@Entity
@Table(name = "asignacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asignacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idsolicitudatencion", nullable = false)
    private SolicitudAtencion solicitudAtencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idgrupoprofesional", nullable = false)
    private GrupoProfesional grupoProfesional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipoasignacion")
    private TipoAsignacion tipoAsignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtiposervicio")
    private TipoServicio tipoServicio;

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

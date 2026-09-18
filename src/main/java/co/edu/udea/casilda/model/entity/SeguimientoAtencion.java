package co.edu.udea.casilda.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seguimientoatencion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoAtencion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idatencion", nullable = false)
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtiposeguimiento", nullable = false)
    private TipoSeguimiento tipoSeguimiento;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idaccion", nullable = false)
    private Accion accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idactividad", nullable = false)
    private Actividad actividad;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idestadoseguimiento", nullable = false)
    private EstadoSeguimiento estadoSeguimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idmotivoestado", nullable = false)
    private MotivoEstadoSeguimiento motivoEstadoSeguimiento;

    @OneToMany(mappedBy = "seguimientoAtencion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ArchivoSeguimientoAtencion> archivos = new ArrayList<>();
}

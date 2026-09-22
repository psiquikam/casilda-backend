package co.edu.udea.casilda.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad Hecho - Registra los hechos asociados a un caso de atencion.
 */
@Entity
@Table(name = "hecho")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hecho extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcaso", nullable = false)
    private Caso caso;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "lugar", nullable = false)
    private String lugar;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;
}

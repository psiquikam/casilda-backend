package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad maestro para los motivos del estado de la cita.
 */
@Entity
@Table(name = "motivoestadocita")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MotivoEstadoCita extends Auditable {

    @Id
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;
}

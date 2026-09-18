package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad maestro para estados de solicitud de atención
 */
@Entity
@Table(name = "estadosolicitud")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoSolicitud extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}

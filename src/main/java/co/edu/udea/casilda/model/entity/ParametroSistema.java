package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad ParametroSistema - Parámetros configurables del sistema.
 */
@Entity
@Table(name = "parametrosistema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametroSistema extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String clave;

    @Column(nullable = false)
    private String valor;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rutaactivacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaActivacion extends Auditable {
    @Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;
}

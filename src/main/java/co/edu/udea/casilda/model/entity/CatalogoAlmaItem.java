package co.edu.udea.casilda.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalogoalmaitem",
        uniqueConstraints = @UniqueConstraint(columnNames = {"catalogo", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
public class CatalogoAlmaItem extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String catalogo;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, columnDefinition = "text")
    private String etiqueta;

    @Column(nullable = false)
    private Boolean activo;
}

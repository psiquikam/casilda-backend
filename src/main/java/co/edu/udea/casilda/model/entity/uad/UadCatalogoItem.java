package co.edu.udea.casilda.model.entity.uad;

import co.edu.udea.casilda.model.enums.uad.CatalogoUad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ítem de los catálogos UAD (causales de inhibitorio, entidades de remisión, tipos de solicitud de
 * información). Se sirven en {@code /maestros/uad-*} con la forma de {@code MaestroDTO}. Los valores
 * iniciales ({@code database/uad_modulo_entrega1.sql}) son PENDIENTES DE VALIDACIÓN jurídica.
 */
@Entity
@Table(name = "uad_catalogo",
        uniqueConstraints = @UniqueConstraint(name = "uk_uad_catalogo_codigo", columnNames = {"catalogo", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
public class UadCatalogoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "catalogo", nullable = false, length = 30)
    private CatalogoUad catalogo;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}

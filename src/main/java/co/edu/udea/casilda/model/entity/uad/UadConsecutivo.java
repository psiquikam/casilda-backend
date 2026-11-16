package co.edu.udea.casilda.model.entity.uad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador por tipo y año de los consecutivos NDI/NDA (UAD-ID-02, UAD-NOT-02). La clave es
 * {@code NDI-2026} o {@code NDA-2026}; se lee con bloqueo pesimista para que dos radicaciones simultáneas
 * no reciban el mismo número. A diferencia de {@code CasoService}, no depende de leer el máximo existente.
 */
@Entity
@Table(name = "uad_consecutivo")
@Getter
@Setter
@NoArgsConstructor
public class UadConsecutivo {

    @Id
    @Column(name = "clave", length = 10)
    private String clave;

    @Column(name = "ultimo", nullable = false)
    private long ultimo;

    public UadConsecutivo(final String clave) {
        this.clave = clave;
        this.ultimo = 0;
    }
}

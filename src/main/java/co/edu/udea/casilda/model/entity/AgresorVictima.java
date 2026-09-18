package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad AgresorVictima - Datos del presunto agresor vinculados a un caso.
 */
@Entity
@Table(name = "agresorvictima")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgresorVictima extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcaso", nullable = false)
    private Caso caso;

    @Column(name = "primernombre", nullable = false)
    private String primerNombre;

    @Column(name = "segundonombre")
    private String segundoNombre;

    @Column(name = "primerapellido", nullable = false)
    private String primerApellido;

    @Column(name = "segundoapellido")
    private String segundoApellido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idvinculoudea", nullable = false)
    private VinculoUdeA vinculoUdeA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idvinvuloagresorvictima", nullable = false)
    private VinculoAgresorVictima vinculoAgresorVictima;
}

package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "presuntoagresor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresuntoAgresor extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idsolicitudatencion", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SolicitudAtencion solicitudAtencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcaso")
    private Caso caso;

    @Column(name = "identificacion", length = 100)
    private String identificacion;

    @Column(name = "nombre", length = 500)
    private String nombre;

    @Column(name = "correo", length = 254)
    private String correo;

    @Column(name = "remitir_uad", length = 10)
    private String remitirUad;

    @Column(name = "otro_vinculo_udea", length = 255)
    private String otroVinculoUdeA;

    @Column(name = "otro_vinculo_agresor_victima", length = 255)
    private String otroVinculoAgresorVictima;

    @Column(name = "primernombre")
    private String primerNombre;

    @Column(name = "segundonombre")
    private String segundoNombre;

    @Column(name = "primerapellido")
    private String primerApellido;

    @Column(name = "segundoapellido")
    private String segundoApellido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idvinculoudea")
    private VinculoUdeA vinculoUdeA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idvinculoagresorvictima")
    private VinculoAgresorVictima vinculoAgresorVictima;
}

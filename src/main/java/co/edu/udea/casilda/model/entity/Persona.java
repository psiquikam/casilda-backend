package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una Persona en el sistema CASILDA.
 * Contiene información demográfica personal requerida para los casos.
 */
@Entity
@Table(name = "persona")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Persona extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "primernombre", nullable = false, length = 100)
    private String primerNombre;

    @Column(name = "segundonombre", length = 100)
    private String segundoNombre;

    @Column(name = "primerapellido", nullable = false, length = 100)
    private String primerApellido;

    @Column(name = "segundoapellido", length = 100)
    private String segundoApellido;

    @Column(name = "numerodocumento", nullable = false, length = 50)
    private String numeroDocumento;

    @Column(name = "fechanacimiento")
    private LocalDateTime fechaNacimiento;

    // Relaciones Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipoidentificacion", nullable = false)
    private TipoIdentificacion tipoIdentificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idsexo")
    private Sexo sexo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idciudadnacimiento", nullable = false)
    private Municipio ciudadNacimiento;

    // Relaciones One-to-Many
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "persona", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiscapacidadPersona> discapacidades = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "persona", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CorreoPersona> correos = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "persona", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TelefonoPersona> telefonos = new ArrayList<>();

    // Método para obtener nombre completo
    public String getNombreCompleto() {
        return primerNombre + " " + (segundoNombre != null ? segundoNombre + " " : "") + 
               primerApellido + " " + (segundoApellido != null ? segundoApellido : "");
    }
}

package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportePresuntoAgresorRequest {

    @Size(max = 20, message = "El documento del presunto agresor no puede exceder 20 caracteres")
    @Pattern(regexp = "^\\d{5,20}$", message = "El documento del presunto agresor debe tener entre 5 y 20 dígitos")
    private String numeroDocumento;

    @Size(max = 60, message = "El primer nombre del presunto agresor no puede exceder 60 caracteres")
    private String primerNombre;

    @Size(max = 60, message = "El segundo nombre del presunto agresor no puede exceder 60 caracteres")
    private String segundoNombre;

    @Size(max = 60, message = "El primer apellido del presunto agresor no puede exceder 60 caracteres")
    private String primerApellido;

    @Size(max = 60, message = "El segundo apellido del presunto agresor no puede exceder 60 caracteres")
    private String segundoApellido;

    private Integer vinculoUniversidadId;
    private Integer vinculoVictimaId;

    @Size(max = 255, message = "El otro vínculo con la Universidad no puede exceder 255 caracteres")
    private String otroVinculoUniversidad;

    @Size(max = 255, message = "El otro vínculo con la víctima no puede exceder 255 caracteres")
    private String otroVinculoVictima;

    @Size(max = 1000, message = "La información adicional no puede exceder 1.000 caracteres")
    private String informacionAdicional;

    private Boolean remitirUad;
}

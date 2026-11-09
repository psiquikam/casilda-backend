package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVictimaRequest {

    @Size(min = 2, max = 60, message = "El primer nombre de la víctima debe tener entre 2 y 60 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El primer nombre de la víctima solo puede contener letras y espacios")
    private String primerNombre;

    @Size(max = 60, message = "El segundo nombre de la víctima no puede exceder 60 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El segundo nombre de la víctima solo puede contener letras y espacios")
    private String segundoNombre;

    @Size(min = 2, max = 60, message = "El primer apellido de la víctima debe tener entre 2 y 60 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El primer apellido de la víctima solo puede contener letras y espacios")
    private String primerApellido;

    @Size(max = 60, message = "El segundo apellido de la víctima no puede exceder 60 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El segundo apellido de la víctima solo puede contener letras y espacios")
    private String segundoApellido;

    @Email(message = "El correo de la víctima no tiene un formato válido")
    @Size(max = 254, message = "El correo de la víctima no puede exceder 254 caracteres")
    private String correo;

    private Integer identidadGeneroId;
    private Integer cargoId;
}

package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroUsuarioRequest {

    @NotBlank
    @Size(min = 2, max = 50)
    private String primerNombre;

    @Size(max = 50)
    private String segundoNombre;

    @NotBlank
    @Size(min = 2, max = 50)
    private String primerApellido;

    @Size(max = 50)
    private String segundoApellido;

    @NotBlank
    private String tipoDocumento;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[0-9A-Za-z]+$")
    private String documento;

    private Integer vinculoUdeaId;

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{7,15}$")
    private String telefono;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotNull
    @AssertTrue
    private Boolean habeasData;
}

package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrarResultadoCitaRequest {

    @NotBlank
    @Pattern(regexp = "ATENDIDA|INASISTENCIA")
    private String resultado;

    @Size(max = 500)
    private String observaciones;
}

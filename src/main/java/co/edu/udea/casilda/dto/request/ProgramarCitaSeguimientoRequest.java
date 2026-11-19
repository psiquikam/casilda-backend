package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProgramarCitaSeguimientoRequest {

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    private String fechaCita;

    @NotBlank
    @Pattern(regexp = "\\d{2}:\\d{2}")
    private String horaCita;
}

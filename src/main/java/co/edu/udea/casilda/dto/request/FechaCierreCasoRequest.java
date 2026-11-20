package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class FechaCierreCasoRequest {
    @NotNull
    private Instant fechaCierre;
}

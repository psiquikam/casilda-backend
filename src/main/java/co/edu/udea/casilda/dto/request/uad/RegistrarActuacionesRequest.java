package co.edu.udea.casilda.dto.request.uad;

import co.edu.udea.casilda.model.enums.uad.CodigoActuacion;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** {@code POST /uad/casos/{id}/actuaciones}: solo actuaciones ordinarias de la fase actual (UAD-HIS-01 a 07, D-UAD-20). */
public record RegistrarActuacionesRequest(
        @NotEmpty(message = "Marque al menos una actuación.") List<CodigoActuacion> actuaciones,
        @Size(max = 2000, message = "Máximo 2000 caracteres.") String observaciones,
        @NotNull(message = "Falta la versión del caso.") Long version
) {
}

package co.edu.udea.casilda.dto.request.uad;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code POST /uad/casos/{id}/traslado-decision}. {@code confirmaTraslado} debe ser {@code true} (UAD-CAS-10). */
public record TrasladoRequest(
        Boolean confirmaTraslado,
        @Size(max = 2000, message = "Máximo 2000 caracteres.") String observaciones,
        @NotNull(message = "Falta la versión del caso.") Long version
) {
}

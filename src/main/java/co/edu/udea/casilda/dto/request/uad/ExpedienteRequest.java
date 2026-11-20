package co.edu.udea.casilda.dto.request.uad;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code PATCH /uad/casos/{id}/expediente} (UAD-ID-07). {@code null} o vacío deja el expediente sin registrar. */
public record ExpedienteRequest(
        @Size(max = 40, message = "Máximo 40 caracteres.") String expedienteUad,
        @NotNull(message = "Falta la versión del caso.") Long version
) {
}

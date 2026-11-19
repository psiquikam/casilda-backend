package co.edu.udea.casilda.dto.request.uad;

import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import jakarta.validation.constraints.NotNull;

/** {@code POST /uad/noticias/{id}/clasificacion} (UAD-NOT-07/08). */
public record ClasificacionUadRequest(
        @NotNull(message = "Seleccione una vinculación de la lista.") VinculacionInvestigado vinculacionInvestigado,
        @NotNull(message = "Falta la versión de la noticia.") Long version
) {
}

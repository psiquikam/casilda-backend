package co.edu.udea.casilda.dto.request.uad;

import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /uad/noticias-anonimas} (queja anónima, UAD-FOR-12). Sin campos de identidad ni contacto
 * (UAD-NOT-04): si el cliente los enviara, se ignoran de forma explícita (no se guardan ni se rechaza el envío),
 * sin depender de la configuración global de Jackson.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RadicacionNdaRequest(
        @NotBlank(message = "Cuente con algo más de detalle qué pasó, para que la UAD pueda evaluarlo.")
        @Size(min = 20, max = 4000, message = "El relato debe tener entre 20 y 4000 caracteres.")
        String descripcionHechos,
        @Size(max = 300, message = "Máximo 300 caracteres.") String lugarOcurrencia,
        @Size(max = 300, message = "Máximo 300 caracteres.") String presuntoResponsable,
        VinculacionInvestigado vinculacionDeclarada
) {
}

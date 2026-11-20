package co.edu.udea.casilda.dto.request.uad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * {@code POST /uad/casos/{id}/remisiones-competencia} (UAD-REM-01 a 06). Sin número de oficio ni radicado de
 * salida (UAD-REM-03); la fase la toma el servidor del caso.
 */
public record RemisionCompetenciaCasoRequest(
        @NotBlank(message = "Seleccione la entidad destinataria.") String entidadDestinatariaCodigo,
        @NotNull(message = "Indique la fecha de remisión.") LocalDate fechaRemision,
        @Size(max = 2000, message = "Máximo 2000 caracteres.") String justificacion,
        @NotNull(message = "Falta la versión del caso.") Long version
) {
}

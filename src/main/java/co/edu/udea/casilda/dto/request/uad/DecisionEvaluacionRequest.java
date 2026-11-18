package co.edu.udea.casilda.dto.request.uad;

import co.edu.udea.casilda.model.enums.uad.ModoCaso;
import co.edu.udea.casilda.model.enums.uad.TipoDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * {@code POST /uad/noticias/{id}/decisiones}. Exactamente un bloque presente: el de {@code tipo}
 * (bitácora §8: sin decisiones incompatibles en una misma operación). Las reglas cruzadas las aplica
 * {@code UadDecisionValidator}, igual que {@code validarDecision} del frontend.
 */
public record DecisionEvaluacionRequest(
        @NotNull(message = "Indique la decisión.") TipoDecision tipo,
        @NotNull(message = "Falta la versión de la noticia.") Long version,
        @Size(max = 2000, message = "Máximo 2000 caracteres.") String observaciones,
        @Valid ContinuarTramite continuarTramite,
        @Valid SolicitudInformacion solicitudInformacion,
        @Valid Inhibitorio inhibitorio,
        @Valid RemisionCompetencia remisionCompetencia
) {

    public record ContinuarTramite(
            @NotNull(message = "Indique cómo se obtiene el caso Casilda.") ModoCaso modoCaso,
            String casoIdVincular,
            Boolean confirmaMismosHechos,
            @Size(max = 40, message = "Máximo 40 caracteres.") String expedienteUad
    ) {
    }

    public record SolicitudInformacion(
            String tipoSolicitudCodigo,
            @Size(max = 2000, message = "Máximo 2000 caracteres.") String informacionRequerida,
            LocalDate fechaLimite
    ) {
    }

    public record Inhibitorio(
            String causalCodigo,
            @Size(max = 2000, message = "Máximo 2000 caracteres.") String justificacion
    ) {
    }

    /** Sin número de oficio ni radicado de salida (UAD-REM-03); justificación opcional (UAD-REM-02). */
    public record RemisionCompetencia(
            String faseProceso,
            String entidadDestinatariaCodigo,
            LocalDate fechaRemision,
            @Size(max = 2000, message = "Máximo 2000 caracteres.") String justificacion
    ) {
    }
}

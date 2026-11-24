package co.edu.udea.casilda.dto.response;

import java.time.Instant;

public record NotificacionesPsicologiaResponse(
        long novedadesPendientes,
        long recontactosPendientes,
        long slaVencidos,
        Instant ahoraServidor) {
}

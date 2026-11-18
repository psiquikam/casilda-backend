package co.edu.udea.casilda.dto.response;

import java.time.OffsetDateTime;

public record AuditoriaAlmaResponse(
        String idEvento,
        OffsetDateTime fechaHora,
        String usuario,
        String accion,
        String detalle) {
}

package co.edu.udea.casilda.dto.response;

import java.time.Instant;

public record IntentoContactoPsicologiaResponse(
        String idIntento,
        int numeroIntento,
        String protocolo,
        Instant fechaHora,
        String resultado,
        String nota,
        String justificacion,
        String autor) {
}

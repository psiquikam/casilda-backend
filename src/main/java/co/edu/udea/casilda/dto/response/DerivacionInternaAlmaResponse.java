package co.edu.udea.casilda.dto.response;

import co.edu.udea.casilda.model.entity.DerivacionInternaAlma;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record DerivacionInternaAlmaResponse(String idDerivacion, String tipoContexto, String idContexto,
        String destino, String motivo, String estado, Instant fechaEnvio, Instant fechaRegistro,
        boolean peligroInminente, JsonNode consentimiento, String resultado) {
    public static DerivacionInternaAlmaResponse desde(DerivacionInternaAlma derivacion) {
        return new DerivacionInternaAlmaResponse("DER-%04d".formatted(derivacion.getId()), "ATENCION_APH",
                derivacion.getAtencion().getCodigo(), derivacion.getDestino(), derivacion.getMotivo(),
                "REGISTRADA", null, derivacion.getFechaRegistro(), derivacion.isPeligroInminente(),
                derivacion.getConsentimiento(), null);
    }
}

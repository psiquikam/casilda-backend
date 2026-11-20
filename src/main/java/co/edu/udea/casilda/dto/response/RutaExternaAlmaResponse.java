package co.edu.udea.casilda.dto.response;

import co.edu.udea.casilda.model.entity.RutaExternaAlma;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;

public record RutaExternaAlmaResponse(String idRuta, Integer version, String tipoContexto,
        String idContexto, String entidad, String motivo, LocalDate fechaActivacion, String estado,
        String resultado, boolean peligroInminente, JsonNode consentimiento) {
    public static RutaExternaAlmaResponse desde(RutaExternaAlma ruta) {
        return new RutaExternaAlmaResponse("RUT-%04d".formatted(ruta.getId()), ruta.getVersion(),
                "ATENCION_APH", ruta.getAtencion().getCodigo(), ruta.getEntidad(), ruta.getMotivo(),
                ruta.getFechaActivacion(), ruta.getEstado(), ruta.getResultado(), ruta.isPeligroInminente(),
                ruta.getConsentimiento());
    }
}

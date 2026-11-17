package co.edu.udea.casilda.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record BandejaPsicologiaResponse(List<Item> items, int page, int size, long total) {
    public record Persona(String tipoIdentificacion, String numeroIdentificacion, String nombreCompleto) {
    }

    public record Item(String idAtencion, int version, Persona persona, String triaje, String canalIngreso,
                       String estado, Instant fechaDerivacion, String tomadaPor, boolean tomadaPorMi,
                       boolean recontacto, int intentosRealizados, Map<String, Object> sla) {
    }
}

package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Respuesta de un reporte de caso creado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteCasoResponse {

    private Long id;
    private String codigo;
    private Instant fechaRegistro;
    private RutaActivacionReporteResponse rutaActivacion;
}

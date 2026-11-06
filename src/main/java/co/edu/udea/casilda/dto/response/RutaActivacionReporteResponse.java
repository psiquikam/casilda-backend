package co.edu.udea.casilda.dto.response;

import co.edu.udea.casilda.model.enums.EstadoReporteRutaEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ruta solicitada en un reporte de caso.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaActivacionReporteResponse {

    private Integer id;
    private String codigo;
    private String nombre;
    private EstadoReporteRutaEnum estado;
}

package co.edu.udea.casilda.dto.request;

import lombok.Data;

@Data
public class RutaAtencionRequest {
    private Integer idTipoRutaActivacion;
    private Integer idRutaActivacion;
    private String cual;
}

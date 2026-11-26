package co.edu.udea.casilda.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PestanaDocumentacionRequest {
    private Long idCaso;
    private List<HechoRequest> hechos;
    private Integer hacecuantooccurrio;
    private Integer idtiempoocurridounidad;
    private Integer idformaocurrencia;
    private String ambitoOcurrencia;
    private String ambitoOcurrenciaOtro;
    private Integer idciudadhechos;
    private Integer idlugarocurrencia;
    private Boolean violenciabasadagenero;
    private Boolean hechoviolenciaocurrioactividadesmisionales;
    private Integer idactivadmisional;
}

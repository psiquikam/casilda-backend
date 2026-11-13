package co.edu.udea.casilda.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CatalogoAlmaItemResponse {
    private String codigo;
    private String etiqueta;
    private Boolean activo;
}

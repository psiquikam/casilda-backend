package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Response para Atencion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtencionResponse {

    private Long id;
    private Long casoId;
    private String codigoCaso;
}

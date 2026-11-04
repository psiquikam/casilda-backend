package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Response para un profesional del sistema
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesionalResponse {
    private Long id;
    private String nombre;
    private Boolean activo;
}

package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PestanaRegistroAtencionRequest {
    @NotNull
    private Long casoId;
    private Long citaId;
    private Long idAtencion;
    private Integer idTipoServicio;
    private Integer idLugarEntrevista;
    private String archivoConsentimientoNombre;
    private String archivoConsentimientoTipo;
    private String archivoConsentimientoContenido;
}

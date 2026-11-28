package co.edu.udea.casilda.dto.request;

import lombok.Data;

@Data
public class PestanaRegistroAtencionRequest {
    private Long casoId;
    private Long citaId;
    private Long idAtencion;
    private Integer idTipoServicio;
    private Integer idLugarEntrevista;
    private Integer idFormaOcurrencia;
    private String ambitoOcurrencia;
    private String ambitoOcurrenciaOtro;
    private String archivoConsentimientoNombre;
    private String archivoConsentimientoTipo;
    private String archivoConsentimientoContenido;
    private PestanaDatosPersonaRequest datosPersona;
    private PestanaDatosComplementariosRequest datosComplementarios;
}

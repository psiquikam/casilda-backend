package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Response para Cita de atención
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaResponse {

    private Long id;
    private Long solicitudId;
    private String codigoSolicitud;
    private Long casoId;
    private String codigoCaso;
    private boolean solicitudTieneCaso;
    private String tipoAtencion;

    // Datos del solicitante
    private String nombreSolicitante;
    private String tipoDocumento;
    private String documento;

    // Datos de la cita
    private String fechaCita;
    private Integer idEstadoCita;
    private String estadoCita;
    private String motivoEstadoCita;
    private String observaciones;

    // Datos de la solicitud / caso
    private String tipoSolicitud;
    private String unidadAdministrativa;
    private String profesional;
    private String tipoAsignacion;
    private String unidadAcademica;
    private String campus;
    private String identidadGenero;

    // Datos de contacto
    private String celular;
    private String telefonoAlterno;
    private String correoInstitucional;
    private String correoPersonal;
}

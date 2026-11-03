package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO Response para solicitud de acompañamiento creada
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudAcompanamientoResponse {

    private Long id;
    private String codigo;
    private String tipoSolicitud;
    private Integer idEstadoSolicitud;
    private String estado;
    private LocalDateTime fechaCreacion;

    private Integer medioSolicitudId;
    private String medioSolicitud;

    private String observacionesTelefono;
    private String observacionesCorreo;

    // Para las tablas del componente de consulta
    private String unidadAdministrativa;
    private String profesional;
    private String tipoAsignacion;

    // Reparto vigente (null mientras la solicitud no tenga asignación)
    private Integer tipoAsignacionId;
    private Integer tipoServicioId;
    private String tipoServicio;
    private Integer grupoProfesionalId;
    private LocalDate fechaReparto;
    private String observacionesReparto;

    // Hechos
    private String descripcionHechos;
    private String lugarOcurrencia;

    // Vínculo del solicitante con la universidad
    private Integer vinculoUniversidadId;
    private String vinculoUniversidad;

    // Presunta persona agresora
    private String agresorIdentificacion;
    private String agresorNombre;
    private Integer agresorVinculoUdeaId;
    private String agresorVinculoUdea;
    private String agresorOtroVinculoUdea;
    private Integer agresorVinculoVictimaId;
    private String agresorVinculoVictima;
    private String agresorOtroVinculoVictima;
    private String agresorCorreo;
    private String agresorRemitirUad;

    // Datos del solicitante (resumen)
    private String nombreSolicitante;
    private String documentoSolicitante;

    // Datos del solicitante (completos)
    private Integer tipoDocumentoId;
    private String tipoDocumento;
    private String numeroDocumento;
    private String fechaNacimiento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private Integer identidadGeneroId;
    private String identidadGenero;
    private Integer idDepartamentoResidencia;
    private Integer idCiudadResidencia;
    private String direccionResidencia;
    private String celular;
    private String telefonoAlterno;
    private String correoInstitucional;
    private String correoPersonal;
    private List<CorreoBusquedaResponse> correos;
    private List<TelefonoBusquedaResponse> telefonos;

    // Datos del remitente (si aplica)
    private String nombreRemitente;
    private String remitentePrimerNombre;
    private String remitenteSegundoNombre;
    private String remitentePrimerApellido;
    private String remitenteSegundoApellido;
    private Integer remitenteCargoId;
    private String remitenteCargo;
    private Integer remitenteCampusId;
    private String remitenteCampus;
    private Integer remitenteUnidadAdministrativaId;
    private String remitenteUnidadAdministrativa;
    private Integer remitenteUnidadAcademicaId;
    private String remitenteUnidadAcademica;
    private String remitenteFechaSolicitud;
    private Integer remitenteTipoDocumentoId;
    private String remitenteTipoDocumento;
    private String remitenteNumeroDocumento;
}

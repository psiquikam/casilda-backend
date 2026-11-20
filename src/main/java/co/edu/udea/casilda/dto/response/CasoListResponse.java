package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * DTO Response para listado de casos paginados
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CasoListResponse {

    private Long id;
    private String codigo;
    private Long solicitudId;
    private Long citaId;
    private long cantidadAtenciones;

    // Datos de la persona atendida (víctima / solicitante / remitente)
    private String nombreSolicitante;
    private String tipoDocumento;
    private String documento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String fechaNacimiento;
    private String sexo;
    private String etnia;
    private String orientacionSexual;
    private String eps;
    private String regimenSalud;
    private String departamentoNacimiento;
    private String ciudadNacimiento;
    private String departamentoResidencia;
    private String ciudadResidencia;
    private String direccionResidencia;

    // Datos del caso
    private String fechaCaso;
    private Instant fechaCierre;
    private String estadoCaso;
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

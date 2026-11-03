package co.edu.udea.casilda.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO Request para actualizar datos de una solicitud de acompañamiento
 */
@Data
@NoArgsConstructor
public class UpdateSolicitudRequest {
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private Integer tipoDocumentoId;
    private String numeroDocumento;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate fechaNacimiento;

    private Integer identidadGeneroId;
    private Integer vinculoUniversidadId;

    @NotEmpty(message = "Debe registrar al menos un correo electrónico")
    @Valid
    private List<UpdateCorreoSolicitudRequest> correos = new ArrayList<>();

    @NotEmpty(message = "Debe registrar al menos un teléfono")
    @Valid
    private List<UpdateTelefonoSolicitudRequest> telefonos = new ArrayList<>();

    private String remitentePrimerNombre;
    private String remitenteSegundoNombre;
    private String remitentePrimerApellido;
    private String remitenteSegundoApellido;
    private Integer remitenteCargoId;
    private Integer remitenteCampusId;
    private Integer remitenteUnidadAdministrativaId;
    private Integer remitenteUnidadAcademicaId;
    private Integer remitenteTipoDocumentoId;
    private String remitenteNumeroDocumento;

    private String observacionesTelefono;
    private String observacionesCorreo;

    private Integer medioSolicitudId;

    @NotBlank(message = "La descripción de los hechos es obligatoria")
    private String descripcionHechos;

    @Size(max = 500, message = "El lugar de ocurrencia no puede exceder 500 caracteres")
    private String lugarOcurrencia;

    @Size(max = 100, message = "La identificación del agresor no puede exceder 100 caracteres")
    private String agresorIdentificacion;

    @Size(max = 500, message = "El nombre del agresor no puede exceder 500 caracteres")
    private String agresorNombre;

    private Integer agresorVinculoUdeaId;

    @Size(max = 255, message = "El otro vínculo con la UdeA no puede exceder 255 caracteres")
    private String agresorOtroVinculoUdea;

    private Integer agresorVinculoVictimaId;

    @Size(max = 255, message = "El otro vínculo con la víctima no puede exceder 255 caracteres")
    private String agresorOtroVinculoVictima;

    @Email(message = "El correo del agresor debe ser válido")
    @Size(max = 254, message = "El correo del agresor no puede exceder 254 caracteres")
    private String agresorCorreo;

    @Size(max = 10, message = "El campo remitir a UAD no puede exceder 10 caracteres")
    private String agresorRemitirUad;
}

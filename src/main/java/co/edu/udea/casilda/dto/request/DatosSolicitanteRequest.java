package co.edu.udea.casilda.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para capturar datos del solicitante
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatosSolicitanteRequest {

    @NotBlank(message = "El primer nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El primer nombre debe tener entre 2 y 100 caracteres")
    private String primerNombre;

    @Size(max = 100, message = "El segundo nombre no puede exceder 100 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El primer apellido debe tener entre 2 y 100 caracteres")
    private String primerApellido;

    @Size(max = 100, message = "El segundo apellido no puede exceder 100 caracteres")
    private String segundoApellido;

    private Integer campusId;

    private Integer unidadAdministrativaId;

    private Integer unidadAcademicaId;

    @NotNull(message = "El tipo de documento es obligatorio")
    private Integer tipoDocumentoId;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate fechaNacimiento;

    @NotNull(message = "La identidad de género es obligatoria")
    private Integer identidadGeneroId;

    private Integer vinculoUniversidadId;

    @NotEmpty(message = "Debe registrar al menos un correo electrónico")
    @Valid
    private List<CorreoSolicitanteRequest> correos = new ArrayList<>();

    @NotEmpty(message = "Debe registrar al menos un teléfono")
    @Valid
    private List<TelefonoSolicitanteRequest> telefonos = new ArrayList<>();
}

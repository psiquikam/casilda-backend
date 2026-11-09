package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;

import java.time.LocalDate;

/**
 * DTO de entrada para crear un reporte de caso.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteCasoRequest {

    private Integer tipoDocumentoId;

    @Size(max = 20, message = "El número de documento no puede exceder 20 caracteres")
    private String numeroDocumento;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    private String tipoLugar;

    private Integer campusId;

    @Size(max = 120, message = "La facultad no puede exceder 120 caracteres")
    private String facultad;

    @Size(max = 60, message = "El bloque no puede exceder 60 caracteres")
    private String bloque;

    @Size(max = 120, message = "El espacio no puede exceder 120 caracteres")
    private String espacio;

    @Size(max = 255, message = "El detalle del lugar externo no puede exceder 255 caracteres")
    private String lugarDetalleExterno;

    @NotBlank(message = "La descripción de los hechos es obligatoria")
    @Size(max = 4000, message = "La descripción de los hechos no puede exceder 4.000 caracteres")
    private String descripcionHechos;

    @NotNull(message = "La ruta de activación es obligatoria")
    private Integer rutaActivacionId;

    private String tipoReportante;

    @Valid
    private ReporteVictimaRequest victima;

    @Valid
    private ReporteContactoRequest contacto;

    @Valid
    private ReportePresuntoAgresorRequest presuntoAgresor;
}

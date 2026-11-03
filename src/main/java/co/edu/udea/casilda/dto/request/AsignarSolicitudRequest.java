package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO Request para asignar un grupo profesional a una solicitud de acompañamiento
 */
@Data
@NoArgsConstructor
public class AsignarSolicitudRequest {

    @NotNull(message = "El grupo profesional es obligatorio")
    private Integer grupoProfesionalId;

    @NotNull(message = "El tipo de asignación es obligatorio")
    private Integer idTipoAsignacion;

    @NotNull(message = "El tipo de servicio es obligatorio")
    private Integer idTipoServicio;

    @NotBlank(message = "Las observaciones del reparto son obligatorias")
    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    @NotNull(message = "La fecha de reparto es obligatoria")
    private LocalDate fechaReparto;
}

package co.edu.udea.casilda.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO Request para crear solicitud de acompañamiento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAcompanamientoRequest {

    @NotNull(message = "El tipo de solicitud es obligatorio")
    private Integer tipoSolicitudId;

    @NotNull(message = "La opción ¿Cómo desea realizar su solicitud? es obligatoria")
    private Integer medioSolicitudId;

    private String observacionesTelefono;

    private String observacionesCorreo;

    @NotNull(message = "Los datos del solicitante son obligatorios")
    @Valid
    private DatosSolicitanteRequest datosSolicitante;

    @Valid
    private DatosRemitenteRequest datosRemitente;

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

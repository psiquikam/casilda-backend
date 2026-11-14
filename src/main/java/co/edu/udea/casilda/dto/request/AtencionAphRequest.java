package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AtencionAphRequest {

    @NotNull
    private LocalDateTime fechaHora;

    @NotNull
    private Integer idProtocoloAph;

    @NotNull
    private Boolean practicoTriage;

    private Integer idResultadoTriage;

    @Size(max = 2000, message = "La nota APH no puede exceder 2000 caracteres")
    private String notaAph;

    private String motivoNoTriage;

    @NotNull
    private Boolean aceptaPsicologia;

    @NotNull
    private Boolean requiereRemision;
}

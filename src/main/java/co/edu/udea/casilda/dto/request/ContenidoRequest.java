package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO Request para crear/actualizar un contenido destacado del Home,
 * administrado por el rol gestor de contenidos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContenidoRequest {

    @Size(max = 500, message = "La imagen debe tener máximo 500 caracteres")
    private String imagen;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 160, message = "El título debe tener máximo 160 caracteres")
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;

    @NotNull(message = "La vigencia de inicio es obligatoria")
    private Instant vigenciaInicio;

    private Instant vigenciaFin;

    @Pattern(regexp = "^/.*", message = "El enlace debe ser una ruta interna que empiece con '/'")
    private String enlace;

    @NotBlank(message = "La sección es obligatoria")
    @Pattern(regexp = "acciones|informacion", message = "Debe ser 'acciones' o 'informacion'")
    private String seccion;

    private Integer orden;
}

package co.edu.udea.casilda.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtencionAlmaDatos {
    @NotBlank(message = "Indique por qué canal llegó la solicitud.")
    private String canalIngreso;

    @NotNull(message = "Indique los datos de identificación.")
    @Valid
    private Persona persona;

    @Valid
    private Solicitante solicitante;

    @Size(max = 2000, message = "Ingrese máximo 2000 caracteres.")
    private String motivoConsulta;

    @Size(max = 2000, message = "Ingrese máximo 2000 caracteres.")
    private String notaAph;

    private Boolean aceptaPsicologia;

    @Data
    public static class Persona {
        @NotBlank(message = "Seleccione el tipo de documento.")
        private String tipoIdentificacion;

        @NotBlank(message = "Escriba el número de documento.")
        private String numeroIdentificacion;

        @Size(max = 100, message = "Ingrese máximo 100 caracteres.")
        private String nombres;

        @Size(max = 100, message = "Ingrese máximo 100 caracteres.")
        private String apellidos;

        @Pattern(regexp = "^\\+?\\d{7,15}$", message = "Ingrese un teléfono válido.")
        private String telefonoPrincipal;

        @Email(message = "Ingrese un correo válido.")
        @Size(max = 150, message = "Ingrese máximo 150 caracteres.")
        private String correoPersonal;

        @Size(max = 80, message = "Ingrese máximo 80 caracteres.")
        private String ciudad;

        @Min(value = 1, message = "Seleccione un estrato entre 1 y 6.")
        @Max(value = 6, message = "Seleccione un estrato entre 1 y 6.")
        private Integer estrato;
    }

    @Data
    public static class Solicitante {
        @NotNull(message = "Indique si el solicitante es un tercero.")
        private Boolean esTercero;

        @Size(max = 100, message = "Ingrese máximo 100 caracteres.")
        private String nombres;

        @Size(max = 100, message = "Ingrese máximo 100 caracteres.")
        private String apellidos;

        @Pattern(regexp = "^\\+?\\d{7,15}$", message = "Ingrese un teléfono válido.")
        private String telefono;

        @Size(max = 80, message = "Ingrese máximo 80 caracteres.")
        private String parentesco;
    }
}

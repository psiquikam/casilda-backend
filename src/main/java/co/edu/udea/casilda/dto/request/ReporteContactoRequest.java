package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteContactoRequest {

    private Boolean deseaContacto;

    @Email(message = "El correo de contacto no tiene un formato válido")
    @Size(max = 254, message = "El correo de contacto no puede exceder 254 caracteres")
    private String correo;

    @Pattern(regexp = "^\\d{7,12}$", message = "El WhatsApp de contacto debe tener entre 7 y 12 dígitos")
    private String whatsapp;
}

package co.edu.udea.casilda.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class UsuarioUpsertRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    // La contraseña solo es requerida al crear. Al actualizar es opcional.
    private String password;

    @JsonAlias({"roleIds", "rolesIds", "roles"})
    private Set<Integer> idsRoles = new LinkedHashSet<>();

    @Deprecated
    private Integer idRol;

    private Boolean activo;
}

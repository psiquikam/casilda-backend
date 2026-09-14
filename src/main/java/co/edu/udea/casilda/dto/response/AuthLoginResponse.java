package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthLoginResponse {
    private String nombre;
    private String email;
    private String rol;
    private List<String> roles;
    private List<String> authorities;
    private String foto;
    private String token;
}

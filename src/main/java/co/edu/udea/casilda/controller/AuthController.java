package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.AuthLoginRequest;
import co.edu.udea.casilda.dto.request.RegistroUsuarioRequest;
import co.edu.udea.casilda.dto.response.AuthLoginResponse;
import co.edu.udea.casilda.dto.response.RegistroUsuarioResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import co.edu.udea.casilda.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación y obtención de token JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
        @Operation(
            summary = "Iniciar sesión",
            description = "Valida credenciales y retorna token JWT para pruebas en Swagger",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "LoginRequest",
                        summary = "Ejemplo de login",
                        value = """
                            {
                              \"email\": \"admin@udea.edu.co\",
                              \"password\": \"Casilda2024!\"
                            }
                            """
                    )
                )
            )
        )
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(
            summary = "Registrar usuario",
            description = "Crea una cuenta de usuario. Reutiliza la persona existente sin modificarla o crea una nueva si no existe; vinculoUdeaId solo es obligatorio al crearla. habeasData debe ser true."
    )
    public ResponseEntity<RegistroUsuarioResponse> registrar(
            @Valid @RequestBody RegistroUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }
}

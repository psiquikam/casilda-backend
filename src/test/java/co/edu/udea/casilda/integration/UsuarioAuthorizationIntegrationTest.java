package co.edu.udea.casilda.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioAuthorizationIntegrationTest extends IntegrationTestBase {

    @ParameterizedTest
    @ValueSource(strings = {"coordinador@udea.edu.co", "profesional@udea.edu.co",
            "revisor@udea.edu.co", "user@udea.edu.co"})
    void nonAdminRolesCannotListUsers(String email) throws Exception {
        mockMvc.perform(get("/usuarios").header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin@udea.edu.co"})
    void adminCanUseEveryUserEndpoint(String email) throws Exception {
        String token = bearer(email);
        mockMvc.perform(get("/usuarios").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/usuarios/paginado").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/usuarios/999999").header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}

package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationIntegrationTest extends IntegrationTestBase {

    @Test
    void loginReturnsAllRolesAssignedToTheUser() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"admin@udea.edu.co\",\"password\":\"Casilda2024!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.roles", hasItems("ADMIN")));
    }

    @Test
    void publicLoginDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.edu.co\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/maestros/paises")).andExpect(status().isForbidden());
    }
}

package co.edu.udea.casilda.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItems;

class AuthenticationAndAuthorizationIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void loginReturnsJwtAndRole() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"admin@udea.edu.co","password":"Casilda2024!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("admin@udea.edu.co"))
                .andExpect(jsonPath("$.roles", hasItems("ADMIN", "COORDINADOR")));
    }

    @Test
    void userCanReadEndpointGrantedThroughDynamicRoleMapping() throws Exception {
        String token = login("user@udea.edu.co");

        mockMvc.perform(get("/maestros/paises").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("CO"))
                .andExpect(jsonPath("$[0].nombre").value("Colombia"));
    }

    @Test
    void endpointRoleMappingDeniesUserAndAllowsAdmin() throws Exception {
        String userToken = login("user@udea.edu.co");
        mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin@udea.edu.co");
        mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/maestros/paises"))
                .andExpect(status().isForbidden());
    }

    private String login(final String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Casilda2024!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }
}

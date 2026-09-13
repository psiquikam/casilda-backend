package co.edu.udea.casilda.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaestroAuthorizationIntegrationTest extends IntegrationTestBase {

    @ParameterizedTest
    @ValueSource(strings = {"admin@udea.edu.co", "coordinador@udea.edu.co",
            "profesional@udea.edu.co", "revisor@udea.edu.co", "user@udea.edu.co"})
    void everyRoleCanReadCatalogs(String email) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/maestros/paises").header("Authorization", bearer(email)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"coordinador@udea.edu.co", "profesional@udea.edu.co",
            "revisor@udea.edu.co", "user@udea.edu.co"})
    void onlyAdminCanWriteCatalogs(String email) throws Exception {
        mockMvc.perform(post("/maestros/tipos-solicitud")
                        .header("Authorization", bearer(email))
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.api.Test
    void adminCatalogWritePassesAuthorization() throws Exception {
        int response = mockMvc.perform(post("/maestros/tipos-solicitud")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(403, response);
    }
}

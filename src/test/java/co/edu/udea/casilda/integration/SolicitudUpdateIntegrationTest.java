package co.edu.udea.casilda.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitudUpdateIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void updatesExistingSeededSolicitud() throws Exception {
        String token = login();

        mockMvc.perform(put("/solicitudes/acompanamiento/100")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "primerNombre":"Ana María",
                                  "primerApellido":"Gómez López",
                                  "tipoDocumentoId":1,
                                  "numeroDocumento":"100000001",
                                  "identidadGeneroId":1,
                                  "correos":[{"correo":"ana.updated@example.edu.co","tipoId":1}],
                                  "telefonos":[{"telefono":"3111111111","tipoId":1}],
                                  "observacionesTelefono":"Actualizada",
                                  "observacionesCorreo":"Actualizada",
                                  "medioSolicitudId":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.primerNombre").value("Ana María"))
                .andExpect(jsonPath("$.primerApellido").value("Gómez López"))
                .andExpect(jsonPath("$.observacionesTelefono").value("Actualizada"));
    }

    @Test
    void returnsNotFoundWhenUpdatingUnknownSolicitud() throws Exception {
        mockMvc.perform(put("/solicitudes/acompanamiento/999999")
                        .header("Authorization", "Bearer " + login())
                        .contentType(APPLICATION_JSON)
                        .content("{\"primerNombre\":\"Ana\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"admin@udea.edu.co\",\"password\":\"Casilda2024!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }
}

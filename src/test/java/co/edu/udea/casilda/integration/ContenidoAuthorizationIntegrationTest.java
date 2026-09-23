package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el control de acceso del recurso de contenidos: el endpoint
 * público del Home no exige autenticación, y el CRUD administrativo está
 * restringido a ADMIN y GESTOR_CONTENIDO.
 */
class ContenidoAuthorizationIntegrationTest extends IntegrationTestBase {

    @Test
    void endpointPublicoDelHomeNoRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/contenidos/home"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointPublicoDelHomeIgnoraUnTokenValidoSiEstaPresente() throws Exception {
        mockMvc.perform(get("/contenidos/home")
                        .header("Authorization", bearer("user@udea.edu.co")))
                .andExpect(status().isOk());
    }

    @Test
    void listadoAdministrativoSinTokenEsRechazado() throws Exception {
        mockMvc.perform(get("/contenidos"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin@udea.edu.co", "gestor@udea.edu.co"})
    void rolesAutorizadosPuedenLeerYEscribirContenidos(String email) throws Exception {
        mockMvc.perform(get("/contenidos").header("Authorization", bearer(email)))
                .andExpect(status().isOk());

        int postStatus = mockMvc.perform(post("/contenidos")
                        .header("Authorization", bearer(email))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(403, postStatus);
    }

    @ParameterizedTest
    @ValueSource(strings = {"coordinador@udea.edu.co", "profesional@udea.edu.co",
            "revisor@udea.edu.co", "user@udea.edu.co"})
    void rolesSinPermisoNoPuedenAdministrarContenidos(String email) throws Exception {
        mockMvc.perform(get("/contenidos").header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/contenidos")
                        .header("Authorization", bearer(email))
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/contenidos/1").header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
    }
}

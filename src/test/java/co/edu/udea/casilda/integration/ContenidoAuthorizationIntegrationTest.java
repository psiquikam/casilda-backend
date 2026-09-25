package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el control de acceso del recurso de contenidos: el endpoint
 * público del Home no exige autenticación, y el CRUD administrativo está
 * restringido a ADMIN y GESTOR_CONTENIDO.
 */
class ContenidoAuthorizationIntegrationTest extends IntegrationTestBase {

    private static final String CONTENIDO_VALIDO = """
            {
              "titulo": "Contenido de prueba",
              "contenido": "Cuerpo de prueba",
              "vigenciaInicio": "2026-01-01T00:00:00Z",
              "seccion": "informacion"
            }
            """;

    private MockMultipartFile partesContenido() {
        return new MockMultipartFile(
                "contenido", "", APPLICATION_JSON_VALUE, CONTENIDO_VALIDO.getBytes());
    }

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

        int postStatus = mockMvc.perform(multipart("/contenidos")
                        .file(partesContenido())
                        .header("Authorization", bearer(email)))
                .andReturn().getResponse().getStatus();
        assertNotEquals(403, postStatus);
    }

    @ParameterizedTest
    @ValueSource(strings = {"coordinador@udea.edu.co", "profesional@udea.edu.co",
            "revisor@udea.edu.co", "user@udea.edu.co"})
    void rolesSinPermisoNoPuedenAdministrarContenidos(String email) throws Exception {
        mockMvc.perform(get("/contenidos").header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/contenidos")
                        .file(partesContenido())
                        .header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/contenidos/1").header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
    }
}

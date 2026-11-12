package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaestroAuthorizationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(strings = {"/maestros/vinculos-udea", "/maestros/vinculos-agresor-victima"})
    void catalogosDeVinculosSonPublicosSoloParaLectura(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(1)))
                .andExpect(jsonPath("$[?(@.id == 1)].nombre").isNotEmpty());
        mockMvc.perform(get("/api-casilda" + path).contextPath("/api-casilda"))
                .andExpect(status().isOk());
        mockMvc.perform(get(path).header("Authorization", bearer("user@udea.edu.co")))
                .andExpect(status().isOk());
        mockMvc.perform(get(path).header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(path).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(path).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(path)).andExpect(status().isForbidden());
        mockMvc.perform(get(path + "/otro")).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void migracionActivaReglasExistentesYCreaFaltantesSinCambiarOtras() throws Exception {
        jdbcTemplate.update("UPDATE endpoint SET activo = false, publico = false WHERE path = ? AND http_method = 'GET'",
                "/maestros/vinculos-udea");
        jdbcTemplate.update("DELETE FROM endpoint WHERE path = ? AND http_method = 'GET'",
                "/maestros/vinculos-agresor-victima");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");

        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_endpoints_vinculos_publicos.sql"));
        migration.execute(dataSource);
        migration.execute(dataSource);

        for (String path : new String[]{"/maestros/vinculos-udea", "/maestros/vinculos-agresor-victima"}) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
            mockMvc.perform(post(path).contentType(APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/maestros/paises")).andExpect(status().isForbidden());
    }

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

package co.edu.udea.casilda.integration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogoAlmaIntegrationTest extends IntegrationTestBase {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void catalogosSonAutenticadosYValoresProvisionalesNoSeActivan() throws Exception {
        ResourceDatabasePopulator seed = new ResourceDatabasePopulator(
                new FileSystemResource("database/insert_catalogos_aph_provisionales.sql"));
        seed.execute(dataSource);
        seed.execute(dataSource);

        assertEquals(31, jdbcTemplate.queryForObject("SELECT count(*) FROM catalogoalmaitem", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM catalogoalmaitem WHERE activo = true", Integer.class));

        mockMvc.perform(get("/alma/catalogos/estratos")).andExpect(status().isForbidden());
        String auth = bearer("user@udea.edu.co");
        mockMvc.perform(get("/alma/catalogos/estratos").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].codigo", hasItem("1")))
                .andExpect(jsonPath("$[*].activo", hasItem(false)));
        mockMvc.perform(get("/alma/catalogos/tipos-identificacion").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == 'CC')].etiqueta", hasItem("Cédula")))
                .andExpect(jsonPath("$[?(@.codigo == 'CC')].activo", hasItem(true)));
        for (String catalogo : new String[]{"parentescos-solicitante", "destinos-internos",
                "rutas-externas", "resultados-ruta-externa", "motivos-cierre-aph"}) {
            mockMvc.perform(get("/alma/catalogos/" + catalogo).header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].activo", hasItem(false)));
        }
        mockMvc.perform(get("/alma/catalogos/textos-consentimiento").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api-casilda/alma/catalogos/estratos")
                        .contextPath("/api-casilda").header("Authorization", auth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/alma/catalogos/inexistente").header("Authorization", auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CATALOGO_NO_ENCONTRADO"));
        mockMvc.perform(post("/alma/catalogos/estratos").header("Authorization", auth))
                .andExpect(status().isForbidden());

        jdbcTemplate.update("UPDATE catalogoalmaitem SET activo = true, etiqueta = 'Etiqueta aprobada' "
                + "WHERE catalogo = 'estratos' AND codigo = '1'");
        seed.execute(dataSource);
        entityManager.clear();
        mockMvc.perform(get("/alma/catalogos/estratos").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == '1')].etiqueta", hasItem("Etiqueta aprobada")))
                .andExpect(jsonPath("$[?(@.codigo == '1')].activo", hasItem(true)));
    }
}

package co.edu.udea.casilda.integration;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsultaPreviaAlmaDeshabilitadaIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/update_auditoria_recursos.sql"),
                new FileSystemResource("database/update_consulta_previa_aph.sql"))
                .execute(dataSource);
    }

    @Test
    void endpointNoEstaDisponiblePorDefectoInclusoConPermisos() throws Exception {
        mockMvc.perform(get("/alma/personas/consulta").header("Authorization", bearer("admin@udea.edu.co"))
                        .param("tipoIdentificacion", "CC").param("numeroIdentificacion", "100000001"))
                .andExpect(status().isNotFound());
    }
}

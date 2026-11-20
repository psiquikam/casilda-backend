package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LimiteBandejaAphIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @Test
    void limitaPorUsuarioYPermiteConsultarAlTerminarLaVentana() throws Exception {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(new FileSystemResource("database/update_permisos_bandeja_aph.sql"))
                .execute(dataSource);
        String admin = bearer("admin@udea.edu.co");
        String coordinador = bearer("coordinador@udea.edu.co");
        jdbc.update("DELETE FROM limitebandejaaph WHERE idusuario IN (1,2)");
        try {
            for (int i = 0; i < 60; i++) {
                mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", admin))
                        .andExpect(status().isOk());
            }
            mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", admin))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.codigo").value("LIMITE_CONSULTAS"));
            mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", coordinador))
                    .andExpect(status().isOk());
            assertEquals(60, jdbc.queryForObject("SELECT cantidad FROM limitebandejaaph WHERE idusuario=1", Integer.class));
            jdbc.update("UPDATE limitebandejaaph SET inicio=clock_timestamp()-interval '2 minutes' WHERE idusuario=1");
            mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", admin))
                    .andExpect(status().isOk());
            assertEquals(1, jdbc.queryForObject("SELECT cantidad FROM limitebandejaaph WHERE idusuario=1", Integer.class));
        } finally {
            jdbc.update("DELETE FROM limitebandejaaph WHERE idusuario IN (1,2)");
        }
    }
}

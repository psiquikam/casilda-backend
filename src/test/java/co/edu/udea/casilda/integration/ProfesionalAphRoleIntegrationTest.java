package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfesionalAphRoleIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    @Transactional
    void testUserScriptCreatesAccountWithOnlyAphRole() throws Exception {
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('usuario', 'id'), (SELECT MAX(id) FROM usuario))");
        new ResourceDatabasePopulator(new FileSystemResource("database/update_add_rol_profesional_aph.sql"))
                .execute(dataSource);
        jdbcTemplate.update("""
                INSERT INTO usuario (email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
                SELECT 'revisor1@udea.edu.co', password, 'Cuenta fuente', true,
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, idrol
                FROM usuario WHERE id = 4
                """);

        jdbcTemplate.execute(Files.readString(Path.of("database/insert_usuario_prueba_profesional_aph.sql")));

        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM usuario u JOIN usuariorol ur ON ur.idusuario = u.id
                JOIN rol r ON r.id = ur.idrol
                WHERE u.email = 'profesional.aph@casilda.test'
                  AND r.codigo = 'PROFESIONAL_APH' AND u.idrol = r.id
                """, Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM usuariorol ur JOIN usuario u ON u.id = ur.idusuario
                WHERE u.email = 'profesional.aph@casilda.test'
                """, Integer.class));
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"profesional.aph@casilda.test\",\"password\":\"Casilda2024!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("PROFESIONAL_APH")));
    }

    @Test
    @Transactional
    void migrationCreatesRoleIdempotentlyAndEnablesOnlyTheRolesCatalog() throws Exception {
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_profesional_aph.sql"));
        migration.execute(dataSource);
        migration.execute(dataSource);

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM rol WHERE codigo = 'PROFESIONAL_APH'", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM endpointrole er
                JOIN rol r ON r.id = er.idrol
                WHERE r.codigo = 'PROFESIONAL_APH'
                """, Integer.class));

        Integer roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM rol WHERE codigo = 'PROFESIONAL_APH'", Integer.class);
        jdbcTemplate.update("""
                INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
                SELECT 9000, 'aph-test@example.edu.co', password, 'Prueba APH', true,
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?
                FROM usuario WHERE id = 1
                """, roleId);
        jdbcTemplate.update("INSERT INTO usuariorol (idusuario, idrol) VALUES (9000, ?)", roleId);

        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"aph-test@example.edu.co\",\"password\":\"Casilda2024!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("Profesional APH"))
                .andExpect(jsonPath("$.roles", hasItem("PROFESIONAL_APH")))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_PROFESIONAL_APH")));

        String auth = bearer("aph-test@example.edu.co");
        mockMvc.perform(get("/maestros/roles").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == 'PROFESIONAL_APH')].nombre",
                        hasItem("Profesional APH")));
        mockMvc.perform(get("/maestros/paises").header("Authorization", auth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/catalogos/estratos").header("Authorization", auth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/linea-alma/registros").header("Authorization", auth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/maestros/roles")).andExpect(status().isForbidden());
        mockMvc.perform(get("/maestros/roles").header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == 'ADMIN')].nombre", hasItem("Administrador")));
    }

    @Test
    @Transactional
    void migrationDoesNotModifyAnExistingAphRole() {
        jdbcTemplate.update("""
                INSERT INTO rol (id, codigo, nombre, activo)
                VALUES (9000, 'PROFESIONAL_APH', 'Nombre existente', false)
                """);
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(new FileSystemResource("database/update_add_rol_profesional_aph.sql"))
                .execute(dataSource);

        assertEquals("Nombre existente", jdbcTemplate.queryForObject(
                "SELECT nombre FROM rol WHERE id = 9000", String.class));
        assertEquals(Boolean.FALSE, jdbcTemplate.queryForObject(
                "SELECT activo FROM rol WHERE id = 9000", Boolean.class));
    }
}

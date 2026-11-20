package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaestrosEquipoAuthorizationIntegrationTest extends IntegrationTestBase {

    private static final List<String> NUEVOS_ROLES = List.of(
            "TECNICO_APH", "EA_COORDINACION", "ALMA_COORDINACION", "UAD_COORDINACION",
            "SPB_COORDINACION", "SPB_ANALISTA", "UAD_EQUIPO_3", "UAD_EQUIPO_4");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    @Transactional
    void migracionEsIdempotenteYSoloAmpliaLecturaDeMaestros() throws Exception {
        crearRolesYUsuarios();
        int otrosPermisos = contarOtrosPermisos();
        sincronizarSecuenciaEndpoints();
        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_permisos_get_maestros_equipo.sql"));
        migration.execute(dataSource);
        migration.execute(dataSource);

        assertEquals(otrosPermisos, contarOtrosPermisos());
        comprobarPermisosMaestros();
        mockMvc.perform(get("/maestros/paises")).andExpect(status().isForbidden());

        for (String rol : NUEVOS_ROLES) {
            String auth = bearer(rol.toLowerCase() + "@example.edu.co");
            mockMvc.perform(get("/maestros/paises").header("Authorization", auth))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/maestros/catalogos/tipos-solicitud/paginado").header("Authorization", auth))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/maestros/tipos-solicitud").header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/maestros/tipos-solicitud/1").header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/maestros/tipos-solicitud/1").header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/maestros/tipos-solicitud/1").header("Authorization", auth))
                    .andExpect(status().isForbidden());
            for (String path : List.of("/personas/1", "/casos/1", "/usuarios", "/parametros/max-llamadas-contacto",
                    "/linea-alma/registros", "/auditoria")) {
                mockMvc.perform(get(path).header("Authorization", auth)).andExpect(status().isForbidden());
            }
        }
    }

    @Test
    @Transactional
    void configuracionInicialConcedeLecturaAlEquipoSinDarAccesoAPersonas() {
        crearRolesYUsuarios();
        sincronizarSecuenciaEndpoints();
        new ResourceDatabasePopulator(new FileSystemResource("database/initial_data.sql")).execute(dataSource);

        comprobarPermisosMaestros();
        for (String rol : NUEVOS_ROLES) {
            assertEquals(0, jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM endpointrole er
                    JOIN endpoint e ON e.id = er.idendpoint JOIN rol r ON r.id = er.idrol
                    WHERE r.codigo = ? AND e.path = '/personas/**' AND e.http_method = 'GET'
                    """, Integer.class, rol));
        }
    }

    @Test
    @Transactional
    void migracionNoCreaRolesYSePuedeRepetirCuandoSeCreen() {
        sincronizarSecuenciaEndpoints();
        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_permisos_get_maestros_equipo.sql"));
        migration.execute(dataSource);
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM rol WHERE codigo = 'SPB_ANALISTA'", Integer.class));

        jdbcTemplate.update("""
                INSERT INTO rol (id, codigo, nombre, activo)
                VALUES (9100, 'SPB_ANALISTA', 'Analista de riesgos', true)
                """);
        migration.execute(dataSource);
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM endpointrole er
                JOIN endpoint e ON e.id = er.idendpoint JOIN rol r ON r.id = er.idrol
                WHERE r.codigo = 'SPB_ANALISTA' AND e.path = '/maestros/**' AND e.http_method = 'GET'
                """, Integer.class));
    }

    private void crearRolesYUsuarios() {
        for (int i = 0; i < NUEVOS_ROLES.size(); i++) {
            int id = 9100 + i;
            String rol = NUEVOS_ROLES.get(i);
            jdbcTemplate.update("INSERT INTO rol (id, codigo, nombre, activo) VALUES (?, ?, ?, true)",
                    id, rol, rol);
            jdbcTemplate.update("""
                    INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
                    SELECT ?, ?, password, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?
                    FROM usuario WHERE id = 1
                    """, id, rol.toLowerCase() + "@example.edu.co", rol, id);
            jdbcTemplate.update("INSERT INTO usuariorol (idusuario, idrol) VALUES (?, ?)", id, id);
        }
    }

    private void sincronizarSecuenciaEndpoints() {
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
    }

    private int contarOtrosPermisos() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM endpointrole er JOIN endpoint e ON e.id = er.idendpoint
                WHERE NOT (e.path = '/maestros/**' AND e.http_method = 'GET')
                """, Integer.class);
    }

    private void comprobarPermisosMaestros() {
        for (String rol : NUEVOS_ROLES) {
            assertEquals(1, jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM endpointrole er
                    JOIN endpoint e ON e.id = er.idendpoint JOIN rol r ON r.id = er.idrol
                    WHERE r.codigo = ? AND e.path = '/maestros/**' AND e.http_method = 'GET'
                    """, Integer.class, rol));
        }
    }
}

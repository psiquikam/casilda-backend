package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/integration-seed.sql")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MigracionesModulosIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("casilda")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void migracionesDeUadEquipoYAphConservanSusPermisos() throws IOException {
        jdbc.queryForObject("""
                SELECT setval(pg_get_serial_sequence('endpoint', 'id'),
                              (SELECT MAX(id) FROM endpoint), true)
                """, Long.class);

        ResourceDatabasePopulator uad =
                new ResourceDatabasePopulator(new FileSystemResource("database/uad_modulo_entrega1.sql"));
        ResourceDatabasePopulator equipo = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_schema_resultado_cita.sql"),
                new FileSystemResource("database/update_acceso_solicitud_contacto.sql"));
        ResourceDatabasePopulator aph =
                new ResourceDatabasePopulator(new FileSystemResource("database/migration_alma_aph.sql"));

        List<Map<String, Object>> rolesAlma = null;
        List<Map<String, Object>> endpointsAlma = null;
        List<Map<String, Object>> permisosAlma = null;
        for (int i = 0; i < 2; i++) {
            uad.execute(dataSource);
            jdbc.execute(Files.readString(Path.of("database/update_schema_atencion_cita.sql")));
            equipo.execute(dataSource);
            aph.execute(dataSource);

            assertEquals(1, contar("SELECT COUNT(*) FROM rol WHERE codigo = 'TECNICO_APH' AND activo"));
            assertEquals(1, contar("SELECT COUNT(*) FROM rol WHERE codigo = 'ALMA_COORDINACION' AND activo"));
            assertEquals(1, contar("SELECT COUNT(*) FROM endpoint WHERE path = '/alma/auditoria' "
                    + "AND http_method = 'GET' AND activo AND NOT publico"));
            assertEquals(1, permisos("/alma/auditoria", "GET", "ADMIN"));
            assertEquals(1, permisos("/alma/auditoria", "GET", "ALMA_COORDINACION"));

            List<Map<String, Object>> rolesActuales = jdbc.queryForList("""
                    SELECT id, codigo, nombre, activo FROM rol
                    WHERE codigo IN ('TECNICO_APH', 'ALMA_COORDINACION') ORDER BY codigo
                    """);
            List<Map<String, Object>> endpointsActuales = jdbc.queryForList("""
                    SELECT id, path, http_method, activo, publico FROM endpoint
                    WHERE path LIKE '/alma/%' ORDER BY path, http_method
                    """);
            List<Map<String, Object>> permisosActuales = jdbc.queryForList("""
                    SELECT e.path, e.http_method, r.codigo FROM endpointrole er
                    JOIN endpoint e ON e.id = er.idendpoint
                    JOIN rol r ON r.id = er.idrol
                    WHERE e.path LIKE '/alma/%' ORDER BY e.path, e.http_method, r.codigo
                    """);
            if (i == 0) {
                rolesAlma = rolesActuales;
                endpointsAlma = endpointsActuales;
                permisosAlma = permisosActuales;
            } else {
                assertEquals(rolesAlma, rolesActuales);
                assertEquals(endpointsAlma, endpointsActuales);
                assertEquals(permisosAlma, permisosActuales);
            }
        }

        assertEquals(1, permisos("/uad/noticias", "GET", "UAD_EQUIPO_3"));
        assertEquals(0, permisos("/uad/noticias", "GET", "UAD_COORDINACION"));
        assertEquals(1, permisos("/maestros/**", "GET", "UAD_EQUIPO_3"));
        assertEquals(1, permisos("/alma/remisiones-aph", "POST", "UAD_EQUIPO_3"));
        assertEquals(1, permisos("/citas/caso/{casoId}/seguimientos", "POST", "PROFESIONAL"));
        assertEquals(0, permisos("/solicitudes/**", "POST", "PROFESIONAL"));
        assertEquals(1, contar("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema=current_schema() AND table_name='limitebandejaaph'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'atencion_cita_unique_idx'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM estadocita WHERE id = 4 AND nombre = 'Atendida'"));
    }

    private int permisos(String path, String metodo, String rol) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM endpointrole er
                JOIN endpoint e ON e.id = er.idendpoint
                JOIN rol r ON r.id = er.idrol
                WHERE e.path = ? AND e.http_method = ? AND r.codigo = ?
                """, Integer.class, path, metodo, rol);
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}

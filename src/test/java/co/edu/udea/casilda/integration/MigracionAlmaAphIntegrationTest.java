package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MigracionAlmaAphIntegrationTest extends IntegrationTestBase {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void migracionConsolidadaEsIdempotenteYConservaPermisosDelFeature() {
        ResourceDatabasePopulator migracion =
                new ResourceDatabasePopulator(new FileSystemResource("database/migration_alma_aph.sql"));

        migracion.execute(dataSource);
        int rolesDespuesDePrimeraEjecucion = contar("SELECT COUNT(*) FROM rol WHERE codigo IN "
                + "('TECNICO_APH', 'ALMA_COORDINACION')");
        int endpointsDespuesDePrimeraEjecucion = contar("SELECT COUNT(*) FROM endpoint "
                + "WHERE path LIKE '/alma/%'");
        int permisosAuditoriaDespuesDePrimeraEjecucion = contar("""
                SELECT COUNT(*)
                FROM endpointrole er
                JOIN endpoint e ON e.id = er.idendpoint
                JOIN rol r ON r.id = er.idrol
                WHERE e.path = '/alma/auditoria'
                  AND e.http_method = 'GET'
                  AND r.codigo IN ('ADMIN', 'ALMA_COORDINACION')
                """);

        migracion.execute(dataSource);

        assertEquals(2, rolesDespuesDePrimeraEjecucion);
        assertEquals(18, endpointsDespuesDePrimeraEjecucion);
        assertEquals(2, permisosAuditoriaDespuesDePrimeraEjecucion);
        assertEquals(rolesDespuesDePrimeraEjecucion,
                contar("SELECT COUNT(*) FROM rol WHERE codigo IN ('TECNICO_APH', 'ALMA_COORDINACION')"));
        assertEquals(endpointsDespuesDePrimeraEjecucion,
                contar("SELECT COUNT(*) FROM endpoint WHERE path LIKE '/alma/%'"));
        assertEquals(permisosAuditoriaDespuesDePrimeraEjecucion, contar("""
                SELECT COUNT(*)
                FROM endpointrole er
                JOIN endpoint e ON e.id = er.idendpoint
                JOIN rol r ON r.id = er.idrol
                WHERE e.path = '/alma/auditoria'
                  AND e.http_method = 'GET'
                  AND r.codigo IN ('ADMIN', 'ALMA_COORDINACION')
                """));
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}

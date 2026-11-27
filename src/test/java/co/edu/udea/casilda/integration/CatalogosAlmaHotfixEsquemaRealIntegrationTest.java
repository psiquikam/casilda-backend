package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ejecuta el hotfix sobre una copia del esquema que genera Hibernate, con sus
 * restricciones y llaves foráneas reales, sin alterar los datos de las demás pruebas.
 */
class CatalogosAlmaHotfixEsquemaRealIntegrationTest extends IntegrationTestBase {
    private static final String ESQUEMA = "hotfix_real";
    private static final Map<String, Integer> OPCIONES = Map.of(
            "sexo", 6, "orientacionsexual", 9, "identidadgenero", 8, "campus", 25, "regimen", 3, "eps", 29);

    @Test
    void reemplazaCatalogosSobreElEsquemaRealSinPerderReferencias() throws Exception {
        String hotfix = Files.readString(Path.of("database/hotfix_catalogos_linea_alma.sql"));
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            try {
                copiarEsquema(statement);
                statement.execute("""
                        INSERT INTO sexo (id, codigo, nombre) VALUES
                            (1, 'M', 'Mujer'), (2, 'H', 'Hombre'),
                            (3, 'NB', 'No binario'), (4, 'PR', 'Prefiere no responder')
                        ON CONFLICT DO NOTHING;
                        INSERT INTO orientacionsexual (id, nombre) VALUES
                            (1, 'Asexual'), (2, 'Bisexual'), (3, 'Heterosexual'), (4, 'Homosexual'),
                            (5, 'Lesbiana'), (6, 'Prefiere no responder'), (7, 'Otra')
                        ON CONFLICT DO NOTHING;
                        INSERT INTO campus (id, nombre, activo) VALUES
                            (2, 'Campus Carepa', false), (7, 'Campus Amalfi', true), (10, 'Campus La Pintada', true)
                        ON CONFLICT DO NOTHING;
                        INSERT INTO regimen (id, nombre) VALUES
                            (1, 'Contributivo'), (2, 'Subsidiado'), (3, 'Especial'), (4, 'No afiliado')
                        ON CONFLICT DO NOTHING;
                        INSERT INTO eps (id, nombre) VALUES
                            (1, 'Sura EPS'), (5, 'Nueva EPS'), (9, 'SOS EPS'), (14, 'Otra'), (15, 'No tiene')
                        ON CONFLICT DO NOTHING;
                        """);
                Map<String, Integer> referenciasAntes = referencias(statement);
                assertTrue(referenciasAntes.values().stream().anyMatch(cantidad -> cantidad > 0));

                statement.execute(hotfix);
                statement.execute(hotfix);

                for (var catalogo : OPCIONES.entrySet()) {
                    try (var result = statement.executeQuery(
                            "SELECT count(*), min(id), max(id) FROM " + catalogo.getKey())) {
                        result.next();
                        assertEquals(catalogo.getValue(), result.getInt(1), catalogo.getKey());
                        assertEquals(1, result.getInt(2), catalogo.getKey());
                        assertEquals(catalogo.getValue(), result.getInt(3), catalogo.getKey());
                    }
                }
                try (var result = statement.executeQuery("""
                        SELECT (SELECT nombre FROM sexo WHERE id = 1), (SELECT nombre FROM campus WHERE id = 1),
                               (SELECT nombre FROM regimen WHERE id = 3), (SELECT nombre FROM eps WHERE id = 3),
                               (SELECT count(*) FROM campus WHERE NOT activo), (SELECT count(*) FROM epsregimen)
                        """)) {
                    result.next();
                    assertEquals("Femenino", result.getString(1));
                    assertEquals("Campus Amalfi", result.getString(2));
                    assertEquals("Especial o de Excepción", result.getString(3));
                    assertEquals("EPS Sura", result.getString(4));
                    assertEquals(0, result.getInt(5));
                    assertEquals(53, result.getInt(6));
                }
                assertEquals(referenciasAntes, referencias(statement));
            } finally {
                statement.execute("ROLLBACK");
                statement.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
            }
        }
    }

    private void copiarEsquema(Statement statement) throws SQLException {
        statement.execute("SET search_path TO public");
        List<String> tablas = lista(statement, "SELECT quote_ident(tablename) FROM pg_tables WHERE schemaname = 'public'");
        List<String> llaves = lista(statement, """
                SELECT format('ALTER TABLE %s ADD CONSTRAINT %I %s',
                              conrelid::regclass, conname, pg_get_constraintdef(oid))
                FROM pg_constraint
                WHERE contype = 'f' AND connamespace = 'public'::regnamespace
                """);
        statement.execute("DROP SCHEMA IF EXISTS " + ESQUEMA + " CASCADE");
        statement.execute("CREATE SCHEMA " + ESQUEMA);
        for (String tabla : tablas) {
            statement.execute("CREATE TABLE %1$s.%2$s (LIKE public.%2$s INCLUDING ALL)".formatted(ESQUEMA, tabla));
            statement.execute("INSERT INTO %1$s.%2$s SELECT * FROM public.%2$s".formatted(ESQUEMA, tabla));
        }
        statement.execute("SET search_path TO " + ESQUEMA);
        for (String llave : llaves) {
            statement.execute(llave);
        }
    }

    /** Cantidad de valores no nulos por cada columna que referencia los catálogos reemplazados. */
    private Map<String, Integer> referencias(Statement statement) throws SQLException {
        List<String> columnas = lista(statement, """
                SELECT restriccion.conrelid::regclass || '.' || quote_ident(columna.attname)
                FROM pg_constraint restriccion
                JOIN pg_attribute columna
                    ON columna.attrelid = restriccion.conrelid AND columna.attnum = restriccion.conkey[1]
                WHERE restriccion.contype = 'f'
                  AND restriccion.connamespace = '%s'::regnamespace
                  AND restriccion.confrelid IN ('sexo'::regclass, 'orientacionsexual'::regclass,
                      'identidadgenero'::regclass, 'campus'::regclass, 'regimen'::regclass, 'eps'::regclass)
                  AND restriccion.conrelid IS DISTINCT FROM to_regclass('epsregimen')
                """.formatted(ESQUEMA));
        Map<String, Integer> referencias = new TreeMap<>();
        for (String columna : columnas) {
            String tabla = columna.substring(0, columna.lastIndexOf('.'));
            String nombre = columna.substring(columna.lastIndexOf('.') + 1);
            try (var result = statement.executeQuery("SELECT count(%s) FROM %s".formatted(nombre, tabla))) {
                result.next();
                referencias.put(columna, result.getInt(1));
            }
        }
        return referencias;
    }

    private List<String> lista(Statement statement, String consulta) throws SQLException {
        List<String> valores = new ArrayList<>();
        try (var result = statement.executeQuery(consulta)) {
            while (result.next()) {
                valores.add(result.getString(1));
            }
        }
        return valores;
    }
}

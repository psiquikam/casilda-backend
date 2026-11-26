package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class CatalogosAlmaHotfixMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void rechazaReasignacionDeIdsYRevierteLaTransaccion() throws Exception {
        String hotfix = Files.readString(Path.of("database/hotfix_catalogos_linea_alma.sql"));
        Map<String, String> valoresQa = Map.of(
                "sexo", "INSERT INTO sexo VALUES (1, 'X', 'Mujer')",
                "orientacionsexual", "INSERT INTO orientacionsexual VALUES (6, 'Prefiere no responder')",
                "identidadgenero", "INSERT INTO identidadgenero VALUES (1, 'Identidad distinta')",
                "campus", "INSERT INTO campus VALUES (1, 'Campus Apartadó')",
                "regimen", "INSERT INTO regimen VALUES (3, 'Especial')",
                "eps", "INSERT INTO eps VALUES (1, 'Sura EPS')");
        for (var entrada : valoresQa.entrySet()) {
            try (var connection = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                 var statement = connection.createStatement()) {
                String schema = "guardia_" + entrada.getKey();
                statement.execute("CREATE SCHEMA " + schema);
                statement.execute("SET search_path TO " + schema);
                statement.execute("""
                        CREATE TABLE sexo (id integer PRIMARY KEY, codigo text UNIQUE NOT NULL, nombre text UNIQUE NOT NULL);
                        CREATE TABLE orientacionsexual (id integer PRIMARY KEY, nombre text NOT NULL);
                        CREATE TABLE identidadgenero (id integer PRIMARY KEY, nombre text NOT NULL);
                        CREATE TABLE campus (id integer PRIMARY KEY, nombre text NOT NULL);
                        CREATE TABLE regimen (id integer PRIMARY KEY, nombre text NOT NULL);
                        CREATE TABLE eps (id integer PRIMARY KEY, nombre text NOT NULL);
                        """);
                statement.execute(entrada.getValue());
                String nombreOriginal;
                try (var result = statement.executeQuery("SELECT nombre FROM " + entrada.getKey())) {
                    result.next();
                    nombreOriginal = result.getString(1);
                }
                SQLException error = assertThrows(SQLException.class, () -> statement.execute(hotfix));
                assertTrue(error.getMessage().contains("No se puede reasignar el ID"));
                assertTrue(error.getMessage().contains(entrada.getKey()));
                statement.execute("ROLLBACK");
                try (var result = statement.executeQuery("SELECT nombre FROM " + entrada.getKey())) {
                    result.next();
                    assertEquals(nombreOriginal, result.getString(1));
                }
            }
        }
    }

    @Test
    void actualizaVinculosYConservaHistoricoAlRepetirHotfixYCargaInicial() throws Exception {
        String hotfix = Files.readString(Path.of("database/hotfix_catalogos_linea_alma.sql"));
        String seed = Files.readString(Path.of("database/insert_catalogos_primer_contacto_psicologia.sql"));
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE tiporeportealma (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE canalcontacto (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE sexo (id integer PRIMARY KEY, codigo text NOT NULL, nombre text NOT NULL);
                    CREATE TABLE orientacionsexual (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE identidadgenero (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE campus (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE regimen (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE eps (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE unidadacademica (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE programa (
                        id integer PRIMARY KEY,
                        nombre text NOT NULL,
                        idunidadacademica integer NOT NULL REFERENCES unidadacademica(id),
                        aplicapregrado boolean NOT NULL,
                        aplicaposgrado boolean NOT NULL
                    );
                    CREATE TABLE vinculoudea (id integer PRIMARY KEY, nombre text NOT NULL);
                    INSERT INTO vinculoudea VALUES
                        (1, 'Estudiante de Pregrado'), (2, 'Estudiante de Posgrado'),
                        (3, 'Egresado Pregrado'), (4, 'Egresado Posgrado'),
                        (5, 'Personal Administrativo'), (6, 'Docente Vinculado'),
                        (7, 'Docente Ocasional'), (8, 'Docente de Cátedra'),
                        (9, 'Docente Cátedra 50'), (10, 'Jubilado'), (11, 'Pensionado'),
                        (12, 'Prestador de Servicios'), (13, 'Contratista'), (14, 'Externo'),
                        (15, 'Otro tipo de vínculo');
                    INSERT INTO unidadacademica VALUES (1, 'Facultad de prueba');
                    INSERT INTO programa VALUES (1, 'Programa vigente', 1, true, true);
                    CREATE TABLE catalogoalmaitem (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        catalogo varchar(80) NOT NULL, codigo varchar(80) NOT NULL,
                        etiqueta text NOT NULL, activo boolean NOT NULL, UNIQUE (catalogo, codigo));
                    INSERT INTO catalogoalmaitem (catalogo, codigo, etiqueta, activo) VALUES
                        ('vinculos-agresor', 'FAMILIAR', 'Familiar', true);
                    """);
            statement.execute(hotfix);
            statement.execute(hotfix);
            statement.execute(seed);
            try (var result = statement.executeQuery("SELECT count(*) FROM vinculoudea")) {
                result.next();
                assertEquals(20, result.getInt(1));
            }
            try (var result = statement.executeQuery("SELECT nombre FROM vinculoudea WHERE id = 9")) {
                result.next();
                assertEquals("Docente Cátedra 50", result.getString(1));
            }
            try (var result = statement.executeQuery("SELECT nombre FROM vinculoudea WHERE id = 15")) {
                result.next();
                assertEquals("Otro tipo de vínculo", result.getString(1));
            }
            try (var result = statement.executeQuery("SELECT count(*) FROM vinculoudea WHERE nombre IN ('Jubilado', 'Pensionado', 'Contratista CIS', 'Estudiante de tecnología', 'Personal no docente', 'No tiene vínculo con la universidad', 'Egresado')")) {
                result.next();
                assertEquals(7, result.getInt(1));
            }
            statement.execute("INSERT INTO vinculoudea VALUES (999, 'Contratista CIS')");
            assertThrows(SQLException.class, () -> statement.execute(hotfix));
            statement.execute("ROLLBACK");
            try (var result = statement.executeQuery("SELECT count(*) FROM epsregimen")) {
                result.next();
                assertEquals(53, result.getInt(1));
            }
            try (var result = statement.executeQuery("""
                    SELECT count(*) FROM programa
                    WHERE nombre = 'No aplica' AND idunidadacademica = 1
                    """)) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
            Map<String, String> vinculos = new TreeMap<>();
            try (var result = statement.executeQuery("""
                    SELECT codigo, etiqueta FROM catalogoalmaitem
                    WHERE catalogo = 'vinculos-agresor' AND activo ORDER BY codigo
                    """)) {
                while (result.next()) {
                    vinculos.put(result.getString(1), result.getString(2));
                }
            }
            assertEquals(Map.ofEntries(
                    Map.entry("AMIGO_AMIGA", "Amigo/amiga"),
                    Map.entry("COMPANERO_ESTUDIO", "Compañero o compañera de estudio"),
                    Map.entry("COMPANERO_TRABAJO", "Compañero o compañera de trabajo"),
                    Map.entry("COMPARTEN_LUGAR_ESTUDIO", "Comparten lugar de estudio"),
                    Map.entry("COMPARTEN_LUGAR_TRABAJO", "Comparten lugar de trabajo"),
                    Map.entry("CONOCIDO", "Conocido"),
                    Map.entry("DESCONOCIDO", "Persona desconocida"),
                    Map.entry("DOCENTE", "Docente"),
                    Map.entry("EMPLEADO_UNIVERSIDAD", "Empleado de la universidad"),
                    Map.entry("EXPAREJA", "Expareja"),
                    Map.entry("FAMILIAR", "Familiar"),
                    Map.entry("JEFE", "Jefe"),
                    Map.entry("NO_TIENE_VINCULO", "No tiene vínculo"),
                    Map.entry("OTRO", "Otro"),
                    Map.entry("PAREJA", "Pareja")), vinculos);
        }
    }
}

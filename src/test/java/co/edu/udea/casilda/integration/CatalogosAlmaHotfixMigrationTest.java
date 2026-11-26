package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        Map<String, String> valoresQa = Map.of("sexo", "INSERT INTO sexo VALUES (1, 'F', 'Valor distinto')");
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
                        CREATE TABLE campus (id integer PRIMARY KEY, nombre text NOT NULL, activo boolean NOT NULL DEFAULT true);
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
                assertTrue(error.getMessage().contains("El código de sexo F ya pertenece a otro valor"));
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
                    CREATE TABLE sexo (id integer PRIMARY KEY, codigo text UNIQUE NOT NULL, nombre text UNIQUE NOT NULL);
                    INSERT INTO sexo VALUES
                        (1, 'M', 'Mujer'), (2, 'H', 'Hombre'),
                        (3, 'NB', 'No binario'), (4, 'PR', 'Prefiere no responder');
                    CREATE TABLE orientacionsexual (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE identidadgenero (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE campus (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE regimen (id integer PRIMARY KEY, nombre text NOT NULL);
                    CREATE TABLE eps (id integer PRIMARY KEY, nombre text NOT NULL);
                    INSERT INTO orientacionsexual VALUES
                        (1, 'Asexual'), (2, 'Bisexual'), (3, 'Heterosexual'),
                        (4, 'Homosexual'), (5, 'Lesbiana'), (6, 'Prefiere no responder'), (7, 'Otra');
                    INSERT INTO identidadgenero VALUES
                        (1, 'Mujer cisgénero'), (2, 'Hombre cisgénero'), (3, 'Mujer trans'),
                        (4, 'Hombre trans'), (5, 'No binaria'), (6, 'Prefiere no responder'),
                        (7, 'Otra'), (8, 'Sin dato');
                    INSERT INTO campus (id, nombre) VALUES
                        (1, 'Campus Apartadó'), (2, 'Campus Carepa'), (3, 'Campus Turbo'),
                        (4, 'Campus Caucasia'), (5, 'Campus Santa Fe de Antioquia'),
                        (6, 'Campus Yarumal'), (7, 'Campus Amalfi'), (8, 'Campus Segovia'),
                        (9, 'Campus Andes'), (10, 'Campus La Pintada'),
                        (11, 'Campus El Carmen de Viboral'), (12, 'Campus Sonsón'),
                        (13, 'Campus Puerto Berrío'), (14, 'Campus Medellín');
                    INSERT INTO regimen VALUES
                        (1, 'Contributivo'), (2, 'Subsidiado'), (3, 'Especial'), (4, 'No afiliado');
                    INSERT INTO eps VALUES
                        (1, 'Sura EPS'), (2, 'Salud Total'), (3, 'Sanitas'),
                        (4, 'Compensar'), (5, 'Nueva EPS'), (6, 'Coomeva EPS'),
                        (7, 'Famisanar'), (8, 'Cafesalud'), (9, 'SOS EPS'),
                        (10, 'Capital Salud'), (11, 'Aliansalud'), (12, 'EPS SAVIA SALUD'),
                        (13, 'Mutual SER'), (14, 'Otra'), (15, 'No tiene');
                    CREATE TABLE persona_prueba (id integer PRIMARY KEY, ideps integer REFERENCES eps(id));
                    INSERT INTO persona_prueba VALUES (1, 1);
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
            try (var result = statement.executeQuery("SELECT id, codigo, nombre FROM sexo WHERE id IN (1, 2) ORDER BY id")) {
                result.next();
                assertEquals(1, result.getInt(1));
                assertEquals("M", result.getString(2));
                assertEquals("Mujer", result.getString(3));
                result.next();
                assertEquals(2, result.getInt(1));
                assertEquals("H", result.getString(2));
                assertEquals("Hombre", result.getString(3));
            }
            try (var result = statement.executeQuery("""
                    SELECT (SELECT nombre FROM campus WHERE id = 1),
                           (SELECT nombre FROM orientacionsexual WHERE id = 6),
                           (SELECT nombre FROM regimen WHERE id = 3)
                    """)) {
                result.next();
                assertEquals("Campus Apartadó", result.getString(1));
                assertEquals("Prefiere no responder", result.getString(2));
                assertEquals("Especial", result.getString(3));
            }
            Map<Integer, String> epsQa = Map.ofEntries(
                    Map.entry(1, "Sura EPS"), Map.entry(2, "Salud Total"),
                    Map.entry(3, "Sanitas"), Map.entry(4, "Compensar"),
                    Map.entry(5, "Nueva EPS"), Map.entry(6, "Coomeva EPS"),
                    Map.entry(7, "Famisanar"), Map.entry(8, "Cafesalud"),
                    Map.entry(9, "SOS EPS"), Map.entry(10, "Capital Salud"),
                    Map.entry(11, "Aliansalud"), Map.entry(12, "EPS SAVIA SALUD"),
                    Map.entry(13, "Mutual SER"), Map.entry(14, "Otra"),
                    Map.entry(15, "No tiene"));
            try (var result = statement.executeQuery("SELECT id, nombre FROM eps WHERE id <= 15")) {
                Map<Integer, String> epsDespues = new TreeMap<>();
                while (result.next()) {
                    epsDespues.put(result.getInt(1), result.getString(2));
                }
                assertEquals(epsQa, epsDespues);
            }
            try (var result = statement.executeQuery("""
                    SELECT e.id, e.nombre FROM persona_prueba p
                    JOIN eps e ON e.id = p.ideps WHERE p.id = 1
                    """)) {
                result.next();
                assertEquals(1, result.getInt(1));
                assertEquals("Sura EPS", result.getString(2));
            }
            try (var result = statement.executeQuery("""
                    SELECT count(*) FROM epsregimen er
                    JOIN eps e ON e.id = er.ideps
                    JOIN regimen r ON r.id = er.idregimen
                    WHERE e.nombre = 'Nueva EPS' AND e.id = 5
                      AND r.nombre IN ('Contributivo', 'Subsidiado')
                    """)) {
                result.next();
                assertEquals(2, result.getInt(1));
            }
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
            statement.execute("DELETE FROM vinculoudea WHERE id = 999");
            statement.execute("INSERT INTO eps VALUES (999, 'NUEVA EPS')");
            SQLException epsDuplicada = assertThrows(SQLException.class, () -> statement.execute(hotfix));
            assertTrue(epsDuplicada.getMessage().contains("Hay más de una EPS llamada Nueva EPS"));
            statement.execute("ROLLBACK");
            try (var result = statement.executeQuery("SELECT count(*) FROM epsregimen")) {
                result.next();
                assertEquals(53, result.getInt(1));
            }
            Set<String> contributivo = new HashSet<>();
            try (var result = statement.executeQuery("""
                    SELECT lower(e.nombre) FROM epsregimen er
                    JOIN eps e ON e.id = er.ideps JOIN regimen r ON r.id = er.idregimen
                    WHERE r.nombre = 'Contributivo'
                    """)) {
                while (result.next()) {
                    contributivo.add(result.getString(1));
                }
            }
            assertEquals(Set.of("nueva eps", "eps sanitas", "eps sura", "salud total eps",
                    "compensar eps", "famisanar", "coosalud eps", "mutual ser", "savia salud",
                    "capital salud", "aliansalud", "comfenalco valle", "servicio occidental de salud (sos)",
                    "emssanar", "asmet salud", "cajacopi eps", "capresoca", "salud mía",
                    "eps familiar de colombia", "anas wayuu", "asociación indígena del cauca - aic",
                    "dusakawi", "mallamas", "pijaos salud"), contributivo);
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

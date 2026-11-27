package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class CatalogosAlmaHotfixMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final List<String> EPS_GENERALES = List.of("Nueva EPS", "EPS Sanitas", "EPS Sura",
            "Salud Total EPS", "Compensar EPS", "Famisanar", "Coosalud EPS", "Mutual Ser", "Savia Salud",
            "Capital Salud", "Aliansalud", "Comfenalco Valle", "Servicio Occidental de Salud (SOS)", "Emssanar",
            "Asmet Salud", "Cajacopi EPS", "Capresoca", "Salud Mía", "EPS Familiar de Colombia", "Anas Wayuu",
            "Asociación Indígena del Cauca - AIC", "Dusakawi", "Mallamas", "Pijaos Salud");
    private static final List<String> EPS_ESPECIALES = List.of(
            "Dirección General de Sanidad Militar (DGSM)", "Dirección de Sanidad de la Policía Nacional (DISAN)",
            "Fondo Nacional de Prestaciones Sociales del Magisterio (FOMAG)", "Departamento de Salud de Ecopetrol",
            "Unidades de Salud de Universidades Públicas");

    /** Devuelve las opciones en orden y comprueba que los ID sean consecutivos desde 1. */
    private static List<String> opciones(Statement statement, String tabla, String expresion) throws SQLException {
        List<String> opciones = new ArrayList<>();
        try (var result = statement.executeQuery("SELECT id, " + expresion + " FROM " + tabla + " ORDER BY id")) {
            while (result.next()) {
                opciones.add(result.getString(2));
                assertEquals(opciones.size(), result.getInt(1));
            }
        }
        return opciones;
    }

    private static List<String> registros(Statement statement) throws SQLException {
        List<String> registros = new ArrayList<>();
        try (var result = statement.executeQuery("""
                SELECT concat_ws('|', s.nombre, COALESCE(o.nombre, 'null'), c.nombre, r.nombre, e.nombre)
                FROM registro_prueba p
                LEFT JOIN sexo s ON s.id = p.idsexo
                LEFT JOIN orientacionsexual o ON o.id = p.idorientacionsexual
                LEFT JOIN campus c ON c.id = p.idcampus
                JOIN regimen r ON r.id = p.idregimen
                JOIN eps e ON e.id = p.ideps
                ORDER BY p.id
                """)) {
            while (result.next()) {
                registros.add(result.getString(1));
            }
        }
        return registros;
    }

    private static List<String> epsDelRegimen(Statement statement, String regimen) throws SQLException {
        List<String> eps = new ArrayList<>();
        try (var result = statement.executeQuery("""
                SELECT e.nombre FROM epsregimen er
                JOIN eps e ON e.id = er.ideps JOIN regimen r ON r.id = er.idregimen
                WHERE r.nombre = '%s' ORDER BY e.id
                """.formatted(regimen))) {
            while (result.next()) {
                eps.add(result.getString(1));
            }
        }
        return eps;
    }

    @Test
    void reemplazaCatalogosConservaReferenciasYEsRepetible() throws Exception {
        String hotfix = Files.readString(Path.of("database/hotfix_catalogos_linea_alma.sql"));
        String seed = Files.readString(Path.of("database/insert_catalogos_primer_contacto_psicologia.sql"));
        String seedAph = Files.readString(Path.of("database/insert_catalogos_aph.sql"));
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
                    CREATE TABLE orientacionsexual (id integer PRIMARY KEY, nombre text UNIQUE NOT NULL);
                    CREATE TABLE identidadgenero (id integer PRIMARY KEY, nombre text UNIQUE NOT NULL);
                    CREATE TABLE campus (id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        nombre text NOT NULL, activo boolean NOT NULL DEFAULT true);
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
                    UPDATE campus SET activo = false WHERE id = 2;
                    INSERT INTO regimen VALUES
                        (1, 'Contributivo'), (2, 'Subsidiado'), (3, 'Especial'), (4, 'No afiliado');
                    INSERT INTO eps VALUES
                        (1, 'Sura EPS'), (2, 'Salud Total'), (3, 'Sanitas'),
                        (4, 'Compensar'), (5, 'Nueva EPS'), (6, 'Coomeva EPS'),
                        (7, 'Famisanar'), (8, 'Cafesalud'), (9, 'SOS EPS'),
                        (10, 'Capital Salud'), (11, 'Aliansalud'), (12, 'EPS SAVIA SALUD'),
                        (13, 'Mutual SER'), (14, 'Otra'), (15, 'No tiene');
                    CREATE TABLE registro_prueba (
                        id integer PRIMARY KEY,
                        idsexo integer REFERENCES sexo(id),
                        idorientacionsexual integer REFERENCES orientacionsexual(id),
                        idcampus integer REFERENCES campus(id),
                        idregimen integer NOT NULL REFERENCES regimen(id),
                        ideps integer NOT NULL REFERENCES eps(id)
                    );
                    INSERT INTO registro_prueba VALUES
                        (1, 1, 6, 1, 3, 1), (2, 4, 7, 7, 1, 5),
                        (3, 3, 1, 10, 4, 14), (4, 2, NULL, 14, 2, 9);
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
                        ('vinculos-agresor', 'FAMILIAR', 'Familiar', true),
                        ('rutas-externas', 'LINEA_123', 'Línea anterior', true),
                        ('rutas-externas', 'LINEA_123_MUJER_ANTIOQUIA', 'Variante existente', false);
                    """);
            statement.execute(hotfix);
            statement.execute(hotfix);
            statement.execute(seed);
            statement.execute(seedAph);
            statement.execute(seedAph);
            try (var result = statement.executeQuery("""
                    SELECT codigo, etiqueta, activo FROM catalogoalmaitem
                    WHERE catalogo = 'rutas-externas'
                      AND codigo IN ('LINEA_123', 'LINEA_123_AGENCIA_MUJER_MEDELLIN',
                                     'LINEA_123_MUJER_ANTIOQUIA', 'MODULO_DIVERSO_LINEA_123_SOCIAL')
                    ORDER BY codigo
                    """)) {
                assertTrue(result.next());
                assertEquals("LINEA_123", result.getString(1));
                assertEquals("Línea anterior", result.getString(2));
                assertTrue(result.getBoolean(3));
                assertTrue(result.next());
                assertEquals("LINEA_123_MUJER_ANTIOQUIA", result.getString(1));
                assertEquals("Variante existente", result.getString(2));
                assertEquals(false, result.getBoolean(3));
                assertEquals(false, result.next());
            }
            statement.execute("DELETE FROM catalogoalmaitem WHERE catalogo = 'rutas-externas' AND codigo = 'LINEA_123'");
            statement.execute(seedAph);
            try (var result = statement.executeQuery("""
                    SELECT etiqueta, activo FROM catalogoalmaitem
                    WHERE catalogo = 'rutas-externas' AND codigo = 'LINEA_123'
                    """)) {
                assertTrue(result.next());
                assertEquals("Línea 123", result.getString(1));
                assertTrue(result.getBoolean(2));
            }
            assertEquals(List.of("F:Femenino", "M:Masculino", "I:Intersexual", "T:Tránsgenero",
                    "PR:Prefiere no responder", "SD:Sin dato"), opciones(statement, "sexo", "codigo || ':' || nombre"));
            assertEquals(List.of("Asexual", "Bisexual", "Heterosexual", "Homosexual", "Lesbiana", "Pansexual",
                    "Otra", "Prefiere no responder", "Sin dato"), opciones(statement, "orientacionsexual", "nombre"));
            assertEquals(List.of("Mujer cisgénero", "Hombre cisgénero", "Mujer trans", "Hombre trans",
                    "No binaria", "Prefiere no responder", "Otra", "Sin dato"),
                    opciones(statement, "identidadgenero", "nombre"));
            assertEquals(List.of("Campus Amalfi", "Campus Andes", "Campus Apartadó", "Campus Carepa",
                    "Campus Caucasia", "Campus El Carmen de Viboral", "Campus en Urabá",
                    "Campus Medellín - Antigua Escuela de Derecho", "Campus Medellín - Casas Patrimoniales",
                    "Campus Medellín - Ciudad Universitaria", "Campus Medellín - Ciudadela Robledo",
                    "Campus Medellín - Edificio Antioquia", "Campus Medellín - Edificio de Extensión",
                    "Campus Medellín - Edificio San Ignacio", "Campus Medellín - en el Área de la salud",
                    "Campus Medellín - Sede de Posgrado", "Campus Puerto Berrío", "Campus Santa Fe de Antioquia",
                    "Campus Segovia", "Campus Sonsón", "Campus Turbo", "Campus Yarumal", "No aplica",
                    "Desconocido", "Otro-a"), opciones(statement, "campus", "nombre"));
            assertEquals(List.of("Contributivo", "Subsidiado", "Especial o de Excepción"),
                    opciones(statement, "regimen", "nombre"));
            assertEquals(EPS_GENERALES.size() + EPS_ESPECIALES.size(), opciones(statement, "eps", "nombre").size());
            assertEquals(EPS_GENERALES, opciones(statement, "eps", "nombre").subList(0, EPS_GENERALES.size()));
            try (var result = statement.executeQuery("SELECT count(*) FROM campus WHERE NOT activo")) {
                result.next();
                assertEquals(0, result.getInt(1));
            }
            statement.execute("INSERT INTO campus (nombre) VALUES ('Sede creada después')");
            try (var result = statement.executeQuery("SELECT id FROM campus WHERE nombre = 'Sede creada después'")) {
                result.next();
                assertEquals(26, result.getInt(1));
            }
            statement.execute("DELETE FROM campus WHERE id = 26");
            assertEquals(List.of(
                    "Femenino|Prefiere no responder|Campus Apartadó|Especial o de Excepción|EPS Sura",
                    "Prefiere no responder|Otra|Campus Amalfi|Contributivo|Nueva EPS",
                    "Sin dato|Asexual|Desconocido|Contributivo|Nueva EPS",
                    "Masculino|null|Desconocido|Subsidiado|Servicio Occidental de Salud (SOS)"),
                    registros(statement));
            statement.execute("""
                    INSERT INTO regimen VALUES (4, 'No afiliado');
                    INSERT INTO eps VALUES (999, 'Sura EPS');
                    INSERT INTO registro_prueba VALUES (5, 1, 8, 3, 4, 999);
                    """);
            statement.execute(hotfix);
            assertEquals(List.of("Contributivo", "Subsidiado", "Especial o de Excepción"),
                    opciones(statement, "regimen", "nombre"));
            assertEquals("Femenino|Prefiere no responder|Campus Apartadó|Contributivo|EPS Sura",
                    registros(statement).get(4));
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
            try (var result = statement.executeQuery("SELECT count(*) FROM epsregimen")) {
                result.next();
                assertEquals(53, result.getInt(1));
            }
            assertEquals(EPS_GENERALES, epsDelRegimen(statement, "Contributivo"));
            assertEquals(EPS_GENERALES, epsDelRegimen(statement, "Subsidiado"));
            assertEquals(EPS_ESPECIALES, epsDelRegimen(statement, "Especial o de Excepción"));
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

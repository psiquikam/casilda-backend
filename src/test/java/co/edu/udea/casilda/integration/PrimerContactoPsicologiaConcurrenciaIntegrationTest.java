package co.edu.udea.casilda.integration;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrimerContactoPsicologiaConcurrenciaIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint','id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/migration_primer_contacto_psicologia.sql"))
                .execute(dataSource);
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','concurrencia-contacto','Texto de prueba',true)
                """);
        for (String codigo : List.of("ALMA-2091-000001", "ALMA-2091-000002")) {
            jdbc.update("""
                    INSERT INTO atencionalmaaph (codigo,version,estado,triaje,fechaderivacion,
                        consentimientomedio,consentimientofecha,consentimientoversion,
                        idusuariotomadorpsicologia,datos,fecha_creacion,fecha_actualizacion)
                    VALUES (?,4,'PENDIENTE_PSICOLOGIA','MEDIO',now(),'VERBAL',now(),'concurrencia-contacto',1,
                      '{"canalIngreso":"LLAMADA","persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"1234567898"}}'::jsonb,
                      now(),now())
                    """, codigo);
        }
    }

    @AfterEach
    void limpiar() {
        jdbc.update("""
                DELETE FROM resolucionrecontactopsicologia WHERE idatencion IN
                    (SELECT id FROM atencionalmaaph WHERE codigo IN ('ALMA-2091-000001','ALMA-2091-000002'))
                """);
        jdbc.update("""
                DELETE FROM eventocasopsicologia WHERE idcasopsicologia IN
                    (SELECT id FROM casopsicologia WHERE numerodocumento='1234567898')
                """);
        jdbc.update("""
                DELETE FROM primercontactopsicologia WHERE idatencion IN
                    (SELECT id FROM atencionalmaaph WHERE codigo IN ('ALMA-2091-000001','ALMA-2091-000002'))
                """);
        jdbc.update("DELETE FROM casopsicologia WHERE numerodocumento='1234567898'");
        jdbc.update("""
                DELETE FROM eventoatencionalma WHERE idatencion IN
                    (SELECT id FROM atencionalmaaph WHERE codigo IN ('ALMA-2091-000001','ALMA-2091-000002'))
                """);
        jdbc.update("DELETE FROM atencionalmaaph WHERE codigo IN ('ALMA-2091-000001','ALMA-2091-000002')");
        jdbc.update("DELETE FROM catalogoalmaitem WHERE catalogo='textos-consentimiento' AND codigo='concurrencia-contacto'");
    }

    @Test
    void dosResolucionesSimultaneasDeLaMismaAtencionSoloPersistenUna() throws Exception {
        jdbc.update("""
                INSERT INTO casopsicologia (codigo,version,estado,prioridad,idtipoidentificacion,numerodocumento,
                    idprofesional,fechaapertura,fechaactualizacion)
                VALUES ('CAS-2091-000001',1,'ABIERTO','MEDIA',1,'1234567898',1,now(),now())
                """);
        String auth = bearer("admin@udea.edu.co");
        String body = """
                {"version":4,"decision":"CASO_NUEVO","nota":"Hechos distintos"}
                """;
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            var tareas = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                listos.countDown();
                if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Inicio agotado");
                return mockMvc.perform(post("/alma/psicologia/recontactos/ALMA-2091-000001/resolucion")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(body))
                        .andReturn().getResponse();
            })).toList();
            assertTrue(listos.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            var respuestas = List.of(tareas.get(0).get(25, TimeUnit.SECONDS), tareas.get(1).get(25, TimeUnit.SECONDS));
            assertEquals(List.of(200, 422), respuestas.stream().map(r -> r.getStatus()).sorted().toList());
            assertEquals("RECONTACTO_YA_RESUELTO", objectMapper.readTree(respuestas.stream()
                    .filter(r -> r.getStatus() == 422).findFirst().orElseThrow().getContentAsString())
                    .path("codigo").asText());
            assertEquals(1, jdbc.queryForObject("""
                    SELECT count(*) FROM resolucionrecontactopsicologia r
                    JOIN atencionalmaaph a ON a.id=r.idatencion WHERE a.codigo='ALMA-2091-000001'
                    """, Integer.class));
            assertEquals(5, jdbc.queryForObject("""
                    SELECT version FROM atencionalmaaph WHERE codigo='ALMA-2091-000001'
                    """, Integer.class));
        } finally {
            inicio.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    @Test
    void revierteResolucionSiFallaLaAuditoriaDelCaso() throws Exception {
        jdbc.update("""
                INSERT INTO casopsicologia (codigo,version,estado,prioridad,idtipoidentificacion,numerodocumento,
                    idprofesional,fechaapertura,fechaactualizacion)
                VALUES ('CAS-2091-000001',1,'ABIERTO','MEDIA',1,'1234567898',1,now(),now())
                """);
        jdbc.execute("""
                ALTER TABLE eventocasopsicologia ADD CONSTRAINT ck_test_error_recontacto
                CHECK (accion <> 'VINCULACION')
                """);
        try {
            mockMvc.perform(post("/alma/psicologia/recontactos/ALMA-2091-000001/resolucion")
                    .header("Authorization", bearer("admin@udea.edu.co")).contentType(APPLICATION_JSON)
                    .content("""
                            {"version":4,"decision":"VINCULAR_A_CASO","numeroCaso":"CAS-2091-000001",
                             "nota":"La misma situación"}
                            """)).andExpect(status().isInternalServerError());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM resolucionrecontactopsicologia", Integer.class));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT version FROM casopsicologia WHERE codigo='CAS-2091-000001'", Integer.class));
            assertEquals("PENDIENTE_PSICOLOGIA", jdbc.queryForObject(
                    "SELECT estado FROM atencionalmaaph WHERE codigo='ALMA-2091-000001'", String.class));
            assertEquals(4, jdbc.queryForObject(
                    "SELECT version FROM atencionalmaaph WHERE codigo='ALMA-2091-000001'", Integer.class));
        } finally {
            jdbc.execute("ALTER TABLE eventocasopsicologia DROP CONSTRAINT ck_test_error_recontacto");
        }
    }

    @Test
    void dosAtencionesDeLaMismaPersonaExigenDecisionYNoCreanDosCasos() throws Exception {
        String auth = bearer("admin@udea.edu.co");
        String body = """
                {"version":4,"consentimiento":{"otorgado":true,"medio":"VERBAL",
                 "fechaRegistro":"2026-01-01T10:00:00Z","versionTexto":"concurrencia-contacto"},
                 "valoracionRiesgo":{"nivel":"MEDIO","descripcion":"Descripción de prueba"},
                 "acuerdos":"Acuerdos de prueba","proximoControl":"2099-12-01T10:00:00Z"}
                """;
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            var tareas = List.of("ALMA-2091-000001", "ALMA-2091-000002").stream()
                    .map(codigo -> executor.submit(() -> {
                        listos.countDown();
                        if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Inicio agotado");
                        return mockMvc.perform(post("/alma/psicologia/novedades/{id}/primer-contacto", codigo)
                                .header("Authorization", auth).contentType(APPLICATION_JSON).content(body))
                                .andReturn().getResponse();
                    })).toList();
            assertTrue(listos.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            var respuestas = List.of(tareas.get(0).get(25, TimeUnit.SECONDS), tareas.get(1).get(25, TimeUnit.SECONDS));
            assertEquals(List.of(201, 422), respuestas.stream().map(r -> r.getStatus()).sorted().toList());
            assertEquals("CASO_ACTIVO_EXISTENTE", objectMapper.readTree(respuestas.stream()
                    .filter(r -> r.getStatus() == 422).findFirst().orElseThrow().getContentAsString())
                    .path("codigo").asText());
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM casopsicologia WHERE numerodocumento='1234567898'",
                    Integer.class));
        } finally {
            inicio.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    @Test
    void revierteCasoYAtencionSiFallaLaAuditoria() throws Exception {
        jdbc.execute("""
                ALTER TABLE eventocasopsicologia ADD CONSTRAINT ck_test_error_auditoria
                CHECK (accion <> 'CREACION')
                """);
        try {
            String body = """
                    {"version":4,"consentimiento":{"otorgado":true,"medio":"VERBAL",
                     "fechaRegistro":"2026-01-01T10:00:00Z","versionTexto":"concurrencia-contacto"},
                     "valoracionRiesgo":{"nivel":"MEDIO","descripcion":"Descripción de prueba"},
                     "acuerdos":"Acuerdos de prueba","proximoControl":"2099-12-01T10:00:00Z"}
                    """;
            mockMvc.perform(post("/alma/psicologia/novedades/ALMA-2091-000001/primer-contacto")
                            .header("Authorization", bearer("admin@udea.edu.co"))
                            .contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isInternalServerError());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM casopsicologia WHERE numerodocumento='1234567898'",
                    Integer.class));
            assertEquals(0, jdbc.queryForObject("""
                    SELECT count(*) FROM primercontactopsicologia pc
                    JOIN atencionalmaaph a ON a.id=pc.idatencion WHERE a.codigo='ALMA-2091-000001'
                    """, Integer.class));
            assertEquals("PENDIENTE_PSICOLOGIA", jdbc.queryForObject("""
                    SELECT estado FROM atencionalmaaph WHERE codigo='ALMA-2091-000001'
                    """, String.class));
            assertEquals(4, jdbc.queryForObject("""
                    SELECT version FROM atencionalmaaph WHERE codigo='ALMA-2091-000001'
                    """, Integer.class));
        } finally {
            jdbc.execute("ALTER TABLE eventocasopsicologia DROP CONSTRAINT ck_test_error_auditoria");
        }
    }
}

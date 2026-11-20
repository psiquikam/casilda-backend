package co.edu.udea.casilda.integration;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DerivacionPsicologiaIntegrationTest extends IntegrationTestBase {
    private static final String CONSENTIMIENTO = """
            {"otorgado":true,"medio":"VERBAL","fechaRegistro":"2026-01-01T10:00:00Z","versionTexto":"prueba-v1"}
            """;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        ResourceDatabasePopulator permisos = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_triaje_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_derivacion_psicologia.sql"),
                new FileSystemResource("database/update_permisos_gestion_psicologia.sql"));
        permisos.execute(dataSource);
        permisos.execute(dataSource);
        jdbc.update("""
                INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
                SELECT 9011, 'derivar@example.edu.co', password, 'Prueba APH', true,
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, (SELECT id FROM rol WHERE codigo='TECNICO_APH')
                FROM usuario WHERE id=1
                """);
        jdbc.update("""
                INSERT INTO usuariorol (idusuario, idrol)
                SELECT 9011, id FROM rol WHERE codigo='TECNICO_APH'
                """);
    }

    private String crear(String auth, String nivel) throws Exception {
        String id = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content("""
                                {"canalIngreso":"LLAMADA","motivoConsulta":"Prueba",
                                 "persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"1000000001",
                                            "nombres":"Persona","apellidos":"Prueba"}}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
        if (nivel != null) {
            mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                            .contentType(APPLICATION_JSON)
                            .content("{\"version\":1,\"triaje\":\"" + nivel + "\"}"))
                    .andExpect(status().isOk());
        }
        return id;
    }

    @Test
    @Transactional
    void rechazaDerivacionYBandejaSinAutenticacionSinModificarAtencion() throws Exception {
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        String auth = bearer("derivar@example.edu.co");
        String id = crear(auth, "MEDIO");
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                        .contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/bandeja/psicologia")).andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.estado").value("EN_GESTION_APH"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM eventoatencionalma WHERE accion='DERIVACION'",
                Integer.class));
    }

    @Test
    @Transactional
    void exigeConsentimientoOficialYTriajeAntesDeDerivar() throws Exception {
        String auth = bearer("derivar@example.edu.co");
        String borrador = crear(auth, null);
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", borrador)
                        .header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"version\":1,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("TRIAJE_REQUERIDO"));
        String id = crear(auth, "MEDIO");
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content("{\"version\":2}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("CONSENTIMIENTO_REQUERIDO"));
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                        .header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['consentimiento.versionTexto']").exists());
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                        .header("Authorization", bearer("admin@udea.edu.co")).contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM atencionalmaaph WHERE estado='PENDIENTE_PSICOLOGIA'",
                Integer.class));
    }

    @Test
    @Transactional
    void derivaYEntregaEnBandejaConSlaYPagina() throws Exception {
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        String auth = bearer("derivar@example.edu.co");
        for (String nivel : new String[]{"ALTO", "MEDIO", "BAJO"}) {
            String id = crear(auth, nivel);
            JsonNode result = objectMapper.readTree(mockMvc.perform(
                            post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                                    .header("Authorization", auth).contentType(APPLICATION_JSON)
                                    .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PENDIENTE_PSICOLOGIA"))
                    .andExpect(jsonPath("$.version").value(3))
                    .andReturn().getResponse().getContentAsString());
            int horas = switch (nivel) { case "ALTO" -> 12; case "MEDIO" -> 24; default -> 48; };
            assertEquals(horas, result.path("sla").path("horasLimite").asInt());
            assertEquals(horas, Duration.between(Instant.parse(result.path("fechaDerivacion").asText()),
                    Instant.parse(result.path("sla").path("venceEn").asText())).toHours());
            mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", auth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.consentimiento.versionTexto").value("prueba-v1"))
                    .andExpect(jsonPath("$.aceptaPsicologia").value(true));
            mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                            .header("Authorization", auth).contentType(APPLICATION_JSON)
                            .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
        }
        String psicologia = bearer("profesional@udea.edu.co");
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia)
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].triaje").value("ALTO"))
                .andExpect(jsonPath("$.items[1].triaje").value("MEDIO"))
                .andExpect(jsonPath("$.items[0].recontacto").value(false));
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia)
                        .param("triaje", "BAJO").param("semaforo", "VERDE").param("documento", "0001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].triaje").value("BAJO"));
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", auth))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia).param("triaje", "OTRO"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.triaje").exists());
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", bearer("user@udea.edu.co")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia).param("page", "abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.page").exists());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM eventoatencionalma WHERE accion='DERIVACION'",
                Integer.class));
    }

    @Test
    @Transactional
    void detectaRecontactoConPersonaAtendidaYEstadosAbiertos() throws Exception {
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        jdbc.update("INSERT INTO estadocaso (id,nombre) VALUES (8001,'Abierto activo'),(8002,'Abierto en transición'),(8003,'Cerrado')");
        jdbc.update("INSERT INTO estadocita (id,nombre) VALUES (8001,'Prueba')");
        jdbc.update("INSERT INTO regimen (id,nombre) VALUES (8001,'Prueba')");
        jdbc.update("INSERT INTO eps (id,nombre) VALUES (8001,'Prueba')");
        jdbc.update("UPDATE persona SET numerodocumento='1000000001' WHERE id=101");
        jdbc.update("INSERT INTO cita (id, idsolicitudatencion, fecha, idestadocita) VALUES (8001,100,CURRENT_TIMESTAMP,8001)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps,idestadocaso)
                VALUES (8001,'CAS-PRUEBA',8001,1,8001,8001,8001)
                """);
        String auth = bearer("derivar@example.edu.co");
        String id = crear(auth, "MEDIO");
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                        .header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isCreated());
        entityManager.flush();
        String psicologia = bearer("profesional@udea.edu.co");
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].recontacto").value(true));
        mockMvc.perform(get("/alma/psicologia/novedades/{id}", id).header("Authorization", psicologia))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recontacto").value(true));
        jdbc.update("UPDATE caso SET idestadocaso=8002 WHERE id=8001");
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].recontacto").value(true));
        jdbc.update("UPDATE caso SET idestadocaso=8003 WHERE id=8001");
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].recontacto").value(false));
        jdbc.update("""
                UPDATE atencionalmaaph SET fechaderivacion=CURRENT_TIMESTAMP - interval '15 hours'
                WHERE codigo=?
                """, id);
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia).param("semaforo","AMARILLO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        jdbc.update("""
                UPDATE atencionalmaaph SET fechaderivacion=CURRENT_TIMESTAMP - interval '25 hours'
                WHERE codigo=?
                """, id);
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia).param("semaforo","ROJO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @Transactional
    void tomaYLiberaNovedadConPropietarioVersionYPermisos() throws Exception {
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        jdbc.update("""
                INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
                SELECT 9012, 'psicologa2@example.edu.co', password, 'Psicóloga Dos', true,
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, (SELECT id FROM rol WHERE codigo='PROFESIONAL')
                FROM usuario WHERE id=1
                """);
        jdbc.update("""
                INSERT INTO usuariorol (idusuario, idrol)
                SELECT 9012, id FROM rol WHERE codigo='PROFESIONAL'
                """);
        String auth = bearer("derivar@example.edu.co");
        String id = crear(auth, "MEDIO");
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                        .header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isCreated());

        String psicologia = bearer("profesional@udea.edu.co");
        mockMvc.perform(post("/alma/bandeja/psicologia/{id}/tomar", id)
                        .header("Authorization", psicologia).contentType(APPLICATION_JSON)
                        .content("{\"version\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.tomadaPor").value(org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.tomadaPorMi").value(true));
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", psicologia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tomadaPorMi").value(true));
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", bearer("psicologa2@example.edu.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tomadaPorMi").value(false));

        mockMvc.perform(post("/alma/bandeja/psicologia/{id}/tomar", id)
                        .header("Authorization", psicologia).contentType(APPLICATION_JSON)
                        .content("{\"version\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(4));

        String otraPsicologa = bearer("psicologa2@example.edu.co");
        mockMvc.perform(post("/alma/bandeja/psicologia/{id}/tomar", id)
                        .header("Authorization", otraPsicologa).contentType(APPLICATION_JSON)
                        .content("{\"version\":4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOVEDAD_YA_TOMADA"));
        mockMvc.perform(post("/alma/bandeja/psicologia/{id}/liberar", id)
                        .header("Authorization", otraPsicologa).contentType(APPLICATION_JSON)
                        .content("{\"version\":4}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("NOVEDAD_NO_TOMADA"));

        mockMvc.perform(post("/alma/bandeja/psicologia/{id}/liberar", id)
                        .header("Authorization", psicologia).contentType(APPLICATION_JSON)
                        .content("{\"version\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
        mockMvc.perform(post("/alma/bandeja/psicologia/{id}/liberar", id)
                        .header("Authorization", psicologia).contentType(APPLICATION_JSON)
                        .content("{\"version\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.tomadaPor").value(org.hamcrest.Matchers.nullValue()));

        assertEquals(2, jdbc.queryForObject("""
                SELECT count(*) FROM eventoatencionalma e JOIN atencionalmaaph a ON a.id=e.idatencion
                WHERE a.codigo=? AND e.accion IN ('TOMA_PSICOLOGIA','LIBERACION_PSICOLOGIA')
                """, Integer.class, id));
    }

    @Test
    void dosProfesionalesNoPuedenTomarLaMismaNovedadSimultaneamente() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        String id = null;
        try {
            jdbc.update("""
                    INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                    VALUES ('textos-consentimiento','concurrency-v1','Texto ficticio exclusivo de prueba',true)
                    """);
            String tecnico = bearer("derivar@example.edu.co");
            id = crear(tecnico, "MEDIO");
            mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                            .header("Authorization", tecnico).contentType(APPLICATION_JSON)
                            .content("{\"version\":2,\"consentimiento\":"
                                    + CONSENTIMIENTO.replace("prueba-v1", "concurrency-v1") + "}"))
                    .andExpect(status().isCreated());

            String idAtencion = id;
            List<String> profesionales = List.of(bearer("profesional@udea.edu.co"),
                    bearer("admin@udea.edu.co"));
            CountDownLatch listos = new CountDownLatch(2);
            CountDownLatch inicio = new CountDownLatch(1);
            var tareas = profesionales.stream().map(auth -> executor.submit(() -> {
                listos.countDown();
                if (!inicio.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se pudo iniciar la prueba concurrente");
                }
                return mockMvc.perform(post("/alma/bandeja/psicologia/{id}/tomar", idAtencion)
                                .header("Authorization", auth).contentType(APPLICATION_JSON)
                                .content("{\"version\":3}"))
                        .andReturn().getResponse();
            })).toList();
            assertTrue(listos.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            var respuestas = List.of(tareas.get(0).get(20, TimeUnit.SECONDS),
                    tareas.get(1).get(20, TimeUnit.SECONDS));
            assertEquals(List.of(200, 409), respuestas.stream().map(r -> r.getStatus()).sorted().toList());
            assertEquals("NOVEDAD_YA_TOMADA", objectMapper.readTree(respuestas.stream()
                    .filter(r -> r.getStatus() == 409).findFirst().orElseThrow().getContentAsString())
                    .path("codigo").asText());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
            if (id != null) {
                jdbc.update("""
                        DELETE FROM eventoatencionalma WHERE idatencion IN
                        (SELECT id FROM atencionalmaaph WHERE codigo=?)
                        """, id);
                jdbc.update("DELETE FROM atencionalmaaph WHERE codigo=?", id);
            }
            jdbc.update("DELETE FROM usuariorol WHERE idusuario=9011");
            jdbc.update("DELETE FROM usuario WHERE id=9011");
            jdbc.update("DELETE FROM catalogoalmaitem WHERE catalogo='textos-consentimiento' "
                    + "AND codigo='concurrency-v1'");
        }
    }

    @Test
    @Transactional
    void detallePsicologiaExponeNotasAphYNoNotasClinicas() throws Exception {
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        String auth = bearer("derivar@example.edu.co");
        String id = crear(auth, "MEDIO");
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                        .header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/alma/psicologia/novedades/{id}", id)
                        .header("Authorization", bearer("profesional@udea.edu.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAtencion").value(id))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PSICOLOGIA"))
                .andExpect(jsonPath("$.notasAph").isArray())
                .andExpect(jsonPath("$.notas").doesNotExist())
                .andExpect(jsonPath("$.intentos").isArray())
                .andExpect(jsonPath("$.intentos").isEmpty());
        mockMvc.perform(get("/alma/psicologia/notificaciones")
                        .header("Authorization", bearer("profesional@udea.edu.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.novedadesPendientes").value(1))
                .andExpect(jsonPath("$.recontactosPendientes").value(0))
                .andExpect(jsonPath("$.slaVencidos").value(0))
                .andExpect(jsonPath("$.ahoraServidor").isNotEmpty());
    }

    @Test
    @Transactional
    void registraIntentosConValidacionYCierraAlNoveno() throws Exception {
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        String tecnico = bearer("derivar@example.edu.co");
        String id = crear(tecnico, "MEDIO");
        mockMvc.perform(post("/alma/atenciones-aph/{id}/derivaciones/psicologia", id)
                        .header("Authorization", tecnico).contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isCreated());
        String psicologia = bearer("profesional@udea.edu.co");
        mockMvc.perform(post("/alma/bandeja/psicologia/{id}/tomar", id)
                        .header("Authorization", psicologia).contentType(APPLICATION_JSON)
                        .content("{\"version\":3}"))
                .andExpect(status().isOk());

        int version = 4;
        for (int numero = 1; numero <= 3; numero++) {
            registrarIntento(id, psicologia, version++, null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.intentos").isArray());
        }
        registrarIntento(id, psicologia, version, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.justificacion").exists());
        registrarIntento(id, psicologia, version++, "Se agotaron otras alternativas.");
        for (int numero = 5; numero <= 9; numero++) {
            registrarIntento(id, psicologia, version++, "Se agotaron otras alternativas.")
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(get("/alma/psicologia/novedades/{id}/intentos", id)
                        .header("Authorization", psicologia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[0].numeroIntento").value(1))
                .andExpect(jsonPath("$[0].protocolo").value("PROTOCOLO_1"))
                .andExpect(jsonPath("$[3].protocolo").value("ADICIONAL"))
                .andExpect(jsonPath("$[8].numeroIntento").value(9));
        mockMvc.perform(get("/alma/psicologia/novedades/{id}", id).header("Authorization", psicologia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.cierre.motivo").value("NO_CONTACTO"))
                .andExpect(jsonPath("$.intentos", hasSize(9)));
    }

    private org.springframework.test.web.servlet.ResultActions registrarIntento(
            String id, String auth, int version, String justificacion) throws Exception {
        String justificacionJson = justificacion == null ? "" : ",\"justificacion\":\"" + justificacion + "\"";
        String cuerpo = """
                {"version":%d,"fechaHora":"2026-01-01T10:00:00Z","resultado":"NO_CONTESTA",
                 "nota":"Intento de contacto sin respuesta"%s}
                """.formatted(version, justificacionJson);
        return mockMvc.perform(post("/alma/psicologia/novedades/{id}/intentos", id)
                .header("Authorization", auth).contentType(APPLICATION_JSON).content(cuerpo));
    }

    @Test
    @Transactional
    void rechazaConsentimientoInactivoFechasInvalidasYOtraVersion() throws Exception {
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',false)
                """);
        String auth = bearer("derivar@example.edu.co");
        String id = crear(auth, "ALTO");
        String ruta = "/alma/atenciones-aph/{id}/derivaciones/psicologia";
        mockMvc.perform(post(ruta, id).header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['consentimiento.versionTexto']").exists());
        jdbc.update("UPDATE catalogoalmaitem SET activo=true WHERE codigo='prueba-v1'");
        String futuro = CONSENTIMIENTO.replace("2026-01-01T10:00:00Z", "2999-01-01T10:00:00Z");
        mockMvc.perform(post(ruta, id).header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"consentimiento\":" + futuro + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['consentimiento.fechaRegistro']").exists());
        mockMvc.perform(post(ruta, id).header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"version\":1,\"consentimiento\":" + CONSENTIMIENTO + "}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detalles.versionActual").value(2));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM eventoatencionalma WHERE accion='DERIVACION'",
                Integer.class));
    }
}

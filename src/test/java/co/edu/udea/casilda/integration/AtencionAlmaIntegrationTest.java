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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AtencionAlmaIntegrationTest extends IntegrationTestBase {
    private static final String REQUEST = """
            {"canalIngreso":"LLAMADA","persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"1000000001"},
             "motivoConsulta":"Motivo de prueba","notaAph":"Nota de prueba"}
            """;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void permisosYUsuarios() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_triaje_alma_aph.sql"))
                .execute(dataSource);
        new ResourceDatabasePopulator(new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_triaje_alma_aph.sql"))
                .execute(dataSource);
        jdbc.update("""
                INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
                SELECT 9001, 'aph1@example.edu.co', password, 'APH Uno', true,
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, (SELECT id FROM rol WHERE codigo='TECNICO_APH')
                FROM usuario WHERE id=1
                """);
        jdbc.update("""
                INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
                SELECT 9002, 'aph2@example.edu.co', password, 'APH Dos', true,
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, (SELECT id FROM rol WHERE codigo='TECNICO_APH')
                FROM usuario WHERE id=1
                """);
        jdbc.update("""
                INSERT INTO usuariorol (idusuario, idrol)
                SELECT u.id, r.id FROM usuario u JOIN rol r ON r.codigo='TECNICO_APH'
                WHERE u.id IN (9001,9002)
                """);
    }

    @Test
    @Transactional
    void creaConsultaYActualizaUnBorradorConControlDeVersion() throws Exception {
        String auth = bearer("aph1@example.edu.co");
        JsonNode creado = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAtencion").value(org.hamcrest.Matchers.matchesPattern("ALMA-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.tomadaPor").value("APH Uno"))
                .andExpect(jsonPath("$.gestionSimultanea").value(nullValue()))
                .andExpect(jsonPath("$.notas", hasSize(1)))
                .andReturn().getResponse().getContentAsString());
        String id = creado.path("idAtencion").asText();
        String idNota = creado.path("notas").get(0).path("idNota").asText();

        mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persona.numeroIdentificacion").value("1000000001"));
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"version":1,"persona":{"ciudad":"Medellín","estrato":3},
                                 "notaAph":"Nota actualizada"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.persona.numeroIdentificacion").value("1000000001"))
                .andExpect(jsonPath("$.persona.ciudad").value("Medellín"))
                .andExpect(jsonPath("$.notas[0].idNota").value(idNota))
                .andExpect(jsonPath("$.notas[0].contenido").value("Nota actualizada"));
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"notaAph\":\"Dato anterior\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"))
                .andExpect(jsonPath("$.detalles.versionActual").value(2));
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":2,\"notaAph\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.notas", hasSize(0)));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM eventoatencionalma", Integer.class));
    }

    @Test
    @Transactional
    void restringeLecturaYEscrituraPorRolYPropietario() throws Exception {
        String auth = bearer("aph1@example.edu.co");
        String id = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
        String otro = bearer("aph2@example.edu.co");
        mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", otro))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", otro)
                        .contentType(APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isForbidden());
        for (String email : new String[]{"admin@udea.edu.co", "coordinador@udea.edu.co",
                "profesional@udea.edu.co", "revisor@udea.edu.co"}) {
            mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", bearer(email)))
                    .andExpect(status().isOk());
        }
        for (String email : new String[]{"user@udea.edu.co", "gestor@udea.edu.co"}) {
            mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", bearer(email)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/alma/atenciones-aph").header("Authorization", bearer("revisor@udea.edu.co"))
                        .contentType(APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id)
                        .header("Authorization", bearer("profesional@udea.edu.co"))
                        .contentType(APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/atenciones-aph/{id}", id)).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void rechazaDatosInvalidosYDocumentoInmutable() throws Exception {
        String auth = bearer("aph1@example.edu.co");
        mockMvc.perform(post("/alma/atenciones-aph").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"canalIngreso\":\"REMISION_EA\",\"persona\":{}}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles['persona.tipoIdentificacion']").exists())
                .andExpect(jsonPath("$.detalles.canalIngreso").exists());
        mockMvc.perform(post("/alma/atenciones-aph").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
        mockMvc.perform(post("/alma/atenciones-aph").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"canalIngreso":"LLAMADA","persona":{"tipoIdentificacion":"CC",
                                 "numeroIdentificacion":"1000000001","telefonoPrincipal":"incorrecto"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles['persona.telefonoPrincipal']").value("Ingrese un teléfono válido."));
        String id = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON)
                        .content("{\"version\":1,\"persona\":{\"numeroIdentificacion\":\"9999999999\"}}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"motivoConsulta\":\"Sin versión\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"estado\":\"CERRADO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"persona\":{\"estrato\":\"3\"}}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/alma/atenciones-aph/ALMA-2000-999999").header("Authorization", auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ATENCION_NO_ENCONTRADA"));
    }

    @Test
    @Transactional
    void seedInicialConfiguraLosMismosPermisosSinDuplicarlos() {
        jdbc.update("""
                DELETE FROM endpointrole WHERE idendpoint IN
                    (SELECT id FROM endpoint WHERE path IN
                        ('/alma/atenciones-aph', '/alma/atenciones-aph/{idAtencion}'))
                """);
        jdbc.update("""
                DELETE FROM endpoint WHERE path IN
                    ('/alma/atenciones-aph', '/alma/atenciones-aph/{idAtencion}')
                """);
        ResourceDatabasePopulator seed = new ResourceDatabasePopulator(
                new FileSystemResource("database/initial_data.sql"));
        seed.execute(dataSource);
        seed.execute(dataSource);
        assertEquals(11, jdbc.queryForObject("""
                SELECT count(*) FROM endpointrole er
                JOIN endpoint e ON e.id=er.idendpoint
                WHERE e.path IN ('/alma/atenciones-aph', '/alma/atenciones-aph/{idAtencion}')
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM endpointrole er
                JOIN endpoint e ON e.id=er.idendpoint
                JOIN rol r ON r.id=er.idrol
                WHERE e.path IN ('/alma/atenciones-aph', '/alma/atenciones-aph/{idAtencion}')
                  AND r.codigo IN ('USUARIO', 'GESTOR_CONTENIDO')
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM endpointrole er
                JOIN endpoint e ON e.id=er.idendpoint JOIN rol r ON r.id=er.idrol
                WHERE e.path='/alma/atenciones-aph/{idAtencion}/triaje'
                  AND e.http_method='PUT' AND r.codigo='TECNICO_APH'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM endpointrole er JOIN endpoint e ON e.id=er.idendpoint
                WHERE e.path='/alma/atenciones-aph/{idAtencion}/triaje' AND e.http_method='PUT'
                """, Integer.class));
    }

    @Test
    @Transactional
    void triajeYOmisiónSonPersistentesYExclusivos() throws Exception {
        String auth = bearer("aph1@example.edu.co");
        String id = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"triaje\":\"MEDIO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_GESTION_APH"))
                .andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.triaje").value("MEDIO"))
                .andExpect(jsonPath("$.omisionTriaje").value(nullValue()));
        mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.triaje").value("MEDIO"));
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"peligroInminente\":true,\"justificacionOmision\":\"Situación urgente\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.triaje").value(nullValue()))
                .andExpect(jsonPath("$.omisionTriaje.peligroInminente").value(true))
                .andExpect(jsonPath("$.omisionTriaje.justificacion").value("Situación urgente"));
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":3,\"motivoConsulta\":\"Nuevo motivo\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.omisionTriaje.justificacion").value("Situación urgente"));
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":4,\"triaje\":\"ALTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.omisionTriaje").value(nullValue()));
        mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.triaje").value("ALTO"))
                .andExpect(jsonPath("$.omisionTriaje").value(nullValue()));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM eventoatencionalma e
                JOIN atencionalmaaph a ON a.id=e.idatencion
                WHERE a.codigo=? AND e.accion='CAMBIO_ESTADO'
                """, Integer.class, id));
        assertEquals(5, jdbc.queryForObject("""
                SELECT count(*) FROM eventoatencionalma e
                JOIN atencionalmaaph a ON a.id=e.idatencion WHERE a.codigo=?
                """, Integer.class, id));
    }

    @Test
    @Transactional
    void triajeValidaPermisosVersionEstadoYContenido() throws Exception {
        String auth = bearer("aph1@example.edu.co");
        String id = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
        for (String email : new String[]{"aph2@example.edu.co", "admin@udea.edu.co", "coordinador@udea.edu.co",
                "profesional@udea.edu.co", "revisor@udea.edu.co", "user@udea.edu.co"}) {
            mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", bearer(email))
                            .contentType(APPLICATION_JSON).content("{\"version\":1,\"triaje\":\"BAJO\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"triaje\":\"BAJO\"}"))
                .andExpect(status().isForbidden());
        for (String contenido : new String[]{"{\"version\":1}",
                "{\"version\":1,\"triaje\":\"ALTO\",\"peligroInminente\":true,\"justificacionOmision\":\"Motivo de emergencia\"}"}) {
            mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content(contenido))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("TRIAJE_REQUERIDO"));
        }
        for (String justificacion : new String[]{"corta", "x".repeat(1001)}) {
            mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(
                                    java.util.Map.of("version", 1, "peligroInminente", true,
                                            "justificacionOmision", justificacion))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.justificacionOmision").exists());
        }
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"peligroInminente\":\"true\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.peligroInminente").exists());
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"triaje\":\"INVALIDO\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.triaje").exists());
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":0,\"triaje\":\"ALTO\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
        jdbc.update("UPDATE atencionalmaaph SET estado='CERRADO' WHERE codigo=?", id);
        entityManager.clear();
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"triaje\":\"ALTO\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ATENCION_NO_EDITABLE"));
        String sinMotivo = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("{\"canalIngreso\":\"LLAMADA\",\"persona\":{\"tipoIdentificacion\":\"CC\",\"numeroIdentificacion\":\"1000000001\"}}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", sinMotivo).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"triaje\":\"BAJO\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ATENCION_INCOMPLETA"))
                .andExpect(jsonPath("$.detalles.motivoConsulta").exists());
    }

    @Test
    @Transactional
    void admiteLosLimitesDeJustificacionYTodosLosNiveles() throws Exception {
        String auth = bearer("aph1@example.edu.co");
        String id = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
        int version = 1;
        for (String justificacion : new String[]{"x".repeat(10), "x".repeat(1000)}) {
            mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(
                                    java.util.Map.of("version", version, "peligroInminente", true,
                                            "justificacionOmision", justificacion))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.omisionTriaje.justificacion").value(justificacion));
            version++;
        }
        for (String nivel : new String[]{"ALTO", "MEDIO", "BAJO"}) {
            mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of("version", version, "triaje", nivel))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.triaje").value(nivel));
            version++;
        }
    }
}

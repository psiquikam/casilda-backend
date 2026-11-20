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

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CierreAlmaAphIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_triaje_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_rutas_externas_aph.sql"),
                new FileSystemResource("database/update_permisos_cierre_alma_aph.sql"),
                new FileSystemResource("database/insert_catalogos_aph.sql"));
        scripts.execute(dataSource);
        scripts.execute(dataSource);
        int rol = jdbc.queryForObject("SELECT id FROM rol WHERE codigo='TECNICO_APH'", Integer.class);
        for (int id : new int[]{9801, 9802}) {
            jdbc.update("""
                    INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                    SELECT ?, ?, password, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?
                    FROM usuario WHERE id=1
                    """, id, "cierre" + id + "@example.edu.co", "Tecnico " + id, rol);
            jdbc.update("INSERT INTO usuariorol (idusuario,idrol) VALUES (?,?)", id, rol);
        }
    }

    private String crear(String auth) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content("""
                                {"canalIngreso":"LLAMADA",
                                 "persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"1000000001"},
                                 "motivoConsulta":"Motivo ficticio"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
    }

    private void triaje(String auth, String id) throws Exception {
        mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"triaje\":\"BAJO\"}"))
                .andExpect(status().isOk());
    }

    private String cuerpo(int version) {
        return "{\"version\":" + version + ",\"motivo\":\"ORIENTACION_BRINDADA\",\"observacion\":null}";
    }

    @Test
    @Transactional
    void cierraYConservaDetalleVersionYAuditoria() throws Exception {
        String auth = bearer("cierre9801@example.edu.co");
        String id = crear(auth);
        triaje(auth, id);
        JsonNode respuesta = objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id)
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(cuerpo(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.cierre.motivo").value("ORIENTACION_BRINDADA"))
                .andExpect(jsonPath("$.cierre.observacion").value(nullValue()))
                .andExpect(jsonPath("$.cierre.fecha").isNotEmpty())
                .andReturn().getResponse().getContentAsString());
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cierre.fecha").value(respuesta.path("cierre").path("fecha").asText()));
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(2)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detalles.versionActual").value(3));
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(3)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("ATENCION_NO_EDITABLE"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM eventoatencionalma WHERE accion='CAMBIO_ESTADO'", Integer.class));
    }

    @Test
    @Transactional
    void restringeRolYPropiedadPermiteAdmin() throws Exception {
        String auth = bearer("cierre9801@example.edu.co");
        String otro = bearer("cierre9802@example.edu.co");
        String revisor = bearer("revisor@udea.edu.co");
        String admin = bearer("admin@udea.edu.co");
        String id = crear(auth);
        triaje(auth, id);
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).contentType(APPLICATION_JSON).content(cuerpo(2)))
                .andExpect(status().isForbidden());
        for (String noAutorizado : new String[]{otro, revisor}) {
            mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", noAutorizado)
                            .contentType(APPLICATION_JSON).content(cuerpo(2))).andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(cuerpo(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADO"));
    }

    @Test
    @Transactional
    void noCierraBorradorNiCuandoPsicologiaEstaPendienteOAceptada() throws Exception {
        String auth = bearer("cierre9801@example.edu.co");
        String id = crear(auth);
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(1)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("ATENCION_NO_EDITABLE"));
        triaje(auth, id);
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":2,\"aceptaPsicologia\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(3)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("PSICOLOGIA_PENDIENTE"));
        entityManager.flush();
        jdbc.update("UPDATE atencionalmaaph SET estado='PENDIENTE_PSICOLOGIA' WHERE codigo=?", id);
        entityManager.clear();
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(3)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("PSICOLOGIA_PENDIENTE"));
    }

    @Test
    @Transactional
    void exigeCerrarRutasAntesDeLaAtencion() throws Exception {
        String auth = bearer("cierre9801@example.edu.co");
        String id = crear(auth);
        triaje(auth, id);
        String body = mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"tipoContexto":"ATENCION_APH","idContexto":"%s","entidad":"LINEA_123",
                                 "motivo":"Motivo ficticio","fechaActivacion":"2026-11-17","peligroInminente":true}
                                """.formatted(id)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String ruta = objectMapper.readTree(body).path("idRuta").asText();
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(2)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RUTAS_PENDIENTES"));
        mockMvc.perform(patch("/alma/rutas-externas/{id}", ruta).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"estado\":\"CERRADA\",\"resultado\":\"ATENDIDA\"}"))
                .andExpect(status().isOk());
        entityManager.flush();
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADO"));
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("""
                                {"tipoContexto":"ATENCION_APH","idContexto":"%s","entidad":"LINEA_123",
                                 "motivo":"Motivo ficticio","fechaActivacion":"2026-11-17","peligroInminente":true}
                                """.formatted(id)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    void validaMotivoObservacionYVersion() throws Exception {
        String auth = bearer("cierre9801@example.edu.co");
        String id = crear(auth);
        triaje(auth, id);
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(2).replace("ORIENTACION_BRINDADA", "NO_EXISTE")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.motivo").exists());
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":2,\"motivo\":\"ORIENTACION_BRINDADA\",\"observacion\":\""
                                + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.observacion").exists());
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(cuerpo(1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detalles.versionActual").value(2));
        mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":2,\"motivo\":\"ORIENTACION_BRINDADA\",\"observacion\":\""
                                + "a".repeat(1000) + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cierre.observacion").value("a".repeat(1000)));
    }
}

package co.edu.udea.casilda.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RutaExternaAlmaIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_rutas_externas_aph.sql"),
                new FileSystemResource("database/insert_catalogos_aph.sql"));
        scripts.execute(dataSource);
        scripts.execute(dataSource);
        int rol = jdbc.queryForObject("SELECT id FROM rol WHERE codigo='TECNICO_APH'", Integer.class);
        for (int id : new int[]{9601, 9602}) {
            jdbc.update("""
                    INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                    SELECT ?, ?, password, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?
                    FROM usuario WHERE id=1
                    """, id, "rutas" + id + "@example.edu.co", "Tecnico " + id, rol);
            jdbc.update("INSERT INTO usuariorol (idusuario,idrol) VALUES (?,?)", id, rol);
        }
    }

    private String crearAtencion(String auth) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content("""
                                {"canalIngreso":"LLAMADA",
                                 "persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"1000000001"}}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
    }

    private String solicitud(String id, String entidad, String consentimiento, boolean peligro) {
        return """
                {"tipoContexto":"ATENCION_APH","idContexto":"%s","entidad":"%s",
                 "motivo":"Motivo ficticio","fechaActivacion":"2026-11-17",
                 "peligroInminente":%s,"consentimiento":%s}
                """.formatted(id, entidad, peligro, consentimiento);
    }

    @Test
    @Transactional
    void activaListaActualizaYCierraConVersionYResultado() throws Exception {
        String auth = bearer("rutas9601@example.edu.co");
        String atencion = crearAtencion(auth);
        String consentimiento = """
                {"otorgado":true,"medio":"VERBAL","fechaRegistro":"2026-01-01T10:00:00Z"}
                """;
        JsonNode creada = objectMapper.readTree(mockMvc.perform(post("/alma/rutas-externas")
                        .header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(solicitud(atencion, "FISCALIA", consentimiento, false)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.estado").value("ACTIVADA"))
                .andExpect(jsonPath("$.fechaActivacion").value("2026-11-17"))
                .andExpect(jsonPath("$.consentimiento.medio").value("VERBAL"))
                .andReturn().getResponse().getContentAsString());
        String id = creada.path("idRuta").asText();
        mockMvc.perform(get("/alma/rutas-externas").header("Authorization", auth)
                        .param("tipoContexto", "ATENCION_APH").param("idContexto", atencion))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/alma/atenciones-aph/{id}", atencion).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rutasExternas[0].idRuta").value(id));
        mockMvc.perform(patch("/alma/rutas-externas/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"estado\":\"CERRADA\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.resultado").exists());
        mockMvc.perform(patch("/alma/rutas-externas/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON)
                        .content("{\"version\":1,\"estado\":\"EN_SEGUIMIENTO\",\"resultado\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
        mockMvc.perform(patch("/alma/rutas-externas/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON)
                        .content("{\"version\":1,\"estado\":\"CERRADA\",\"resultado\":\"ATENDIDA\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
        mockMvc.perform(patch("/alma/rutas-externas/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON)
                        .content("{\"version\":2,\"estado\":\"CERRADA\",\"resultado\":\"ATENDIDA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.resultado").value("ATENDIDA"));
        mockMvc.perform(patch("/alma/rutas-externas/{id}", id).header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":3,\"estado\":\"ACTIVADA\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM rutaexternaalma WHERE estado='CERRADA'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM eventoatencionalma WHERE accion='DERIVACION'", Integer.class));
    }

    @Test
    @Transactional
    void controlaAutorizacionPorAtencionYNoExponeRutasAlRevisor() throws Exception {
        String auth = bearer("rutas9601@example.edu.co");
        String otro = bearer("rutas9602@example.edu.co");
        String admin = bearer("admin@udea.edu.co");
        String revisor = bearer("revisor@udea.edu.co");
        String atencion = crearAtencion(auth);
        String payload = solicitud(atencion, "LINEA_123", "null", true);
        mockMvc.perform(post("/alma/rutas-externas").contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", otro)
                        .contentType(APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", revisor)
                        .contentType(APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.peligroInminente").value(true));
        mockMvc.perform(get("/alma/rutas-externas").header("Authorization", otro)
                        .param("tipoContexto", "ATENCION_APH").param("idContexto", atencion))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/atenciones-aph/{id}", atencion).header("Authorization", revisor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rutasExternas", hasSize(0)));
        mockMvc.perform(get("/alma/atenciones-aph/{id}", atencion).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rutasExternas", hasSize(1)));
    }

    @Test
    @Transactional
    void exigeConsentimientoSalvoEmergenciaLinea123YValidaDatos() throws Exception {
        String auth = bearer("rutas9601@example.edu.co");
        String atencion = crearAtencion(auth);
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(solicitud(atencion, "FISCALIA", "null", false)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("CONSENTIMIENTO_REQUERIDO"));
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(solicitud(atencion, "FISCALIA", "null", true)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.peligroInminente").exists());
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(solicitud(atencion, "LINEA_123", "null", true)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content(solicitud(atencion, "INEXISTENTE", "null", true)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.entidad").exists());
        mockMvc.perform(get("/alma/rutas-externas").header("Authorization", auth)
                        .param("tipoContexto", "CASO").param("idContexto", "CAS-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/alma/rutas-externas/RUT-999999").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content("{\"version\":1,\"estado\":\"ACTIVADA\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("RUTA_NO_ENCONTRADA"));
    }

    @Test
    @Transactional
    void siSeIndicaVersionDelConsentimientoDebeSerActivaYLaFechaNoPuedeSerFutura() throws Exception {
        String auth = bearer("rutas9601@example.edu.co");
        String atencion = crearAtencion(auth);
        String consentimiento = """
                {"otorgado":true,"medio":"DIGITAL","fechaRegistro":"2026-01-01T10:00:00Z",
                 "versionTexto":"prueba-v1"}
                """;
        String payload = solicitud(atencion, "FISCALIA", consentimiento, false);
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['consentimiento.versionTexto']").exists());
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(payload.replace("2026-01-01T10:00:00Z", "2999-01-01T10:00:00Z")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['consentimiento.fechaRegistro']").exists());
        mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.consentimiento.versionTexto").value("prueba-v1"));
    }
}

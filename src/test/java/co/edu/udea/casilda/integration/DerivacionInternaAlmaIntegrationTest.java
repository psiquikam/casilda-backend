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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DerivacionInternaAlmaIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_derivaciones_internas_aph.sql"),
                new FileSystemResource("database/insert_catalogos_aph.sql"));
        scripts.execute(dataSource);
        scripts.execute(dataSource);
        int rol = jdbc.queryForObject("SELECT id FROM rol WHERE codigo='TECNICO_APH'", Integer.class);
        for (int id : new int[]{9701, 9702}) {
            jdbc.update("""
                    INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                    SELECT ?, ?, password, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?
                    FROM usuario WHERE id=1
                    """, id, "derivacion" + id + "@example.edu.co", "Tecnico " + id, rol);
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

    private String solicitud(String id, String destino, boolean peligro, String consentimiento) {
        return """
                {"tipoContexto":"ATENCION_APH","idContexto":"%s","destino":"%s",
                 "motivo":"Motivo ficticio","peligroInminente":%s,"consentimiento":%s}
                """.formatted(id, destino, peligro, consentimiento);
    }

    private static final String CONSENTIMIENTO = """
            {"otorgado":true,"medio":"VERBAL","fechaRegistro":"2026-01-01T10:00:00Z"}
            """;

    @Test
    @Transactional
    void registraYListaSinSimularEntregaYAudita() throws Exception {
        String auth = bearer("derivacion9701@example.edu.co");
        String atencion = crearAtencion(auth);
        String payload = solicitud(atencion, "EA", false, CONSENTIMIENTO);
        JsonNode creada = objectMapper.readTree(mockMvc.perform(post("/alma/derivaciones/internas")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.fechaRegistro").isNotEmpty()).andExpect(jsonPath("$.fechaEnvio").doesNotExist())
                .andExpect(jsonPath("$.consentimiento.medio").value("VERBAL"))
                .andReturn().getResponse().getContentAsString());
        String id = creada.path("idDerivacion").asText();
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(solicitud(atencion, "SPYB", true, "null")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.peligroInminente").value(true));
        mockMvc.perform(get("/alma/derivaciones/internas").header("Authorization", auth)
                        .param("tipoContexto", "ATENCION_APH").param("idContexto", atencion))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idDerivacion").value(id))
                .andExpect(jsonPath("$[1].destino").value("SPYB"));
        mockMvc.perform(get("/alma/atenciones-aph/{id}", atencion).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.derivaciones[0].idDerivacion").value(id));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM derivacioninternaalma", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM eventoatencionalma WHERE accion='DERIVACION'", Integer.class));
    }

    @Test
    @Transactional
    void restringeLaGestionAlPropietarioYAdmin() throws Exception {
        String auth = bearer("derivacion9701@example.edu.co");
        String otro = bearer("derivacion9702@example.edu.co");
        String admin = bearer("admin@udea.edu.co");
        String revisor = bearer("revisor@udea.edu.co");
        String atencion = crearAtencion(auth);
        String payload = solicitud(atencion, "EA", false, CONSENTIMIENTO);
        mockMvc.perform(post("/alma/derivaciones/internas").contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        for (String noAutorizado : new String[]{otro, revisor}) {
            mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", noAutorizado)
                            .contentType(APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
            mockMvc.perform(get("/alma/derivaciones/internas").header("Authorization", noAutorizado)
                            .param("tipoContexto", "ATENCION_APH").param("idContexto", atencion))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
        mockMvc.perform(get("/alma/atenciones-aph/{id}", atencion).header("Authorization", revisor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.derivaciones", hasSize(0)));
        mockMvc.perform(get("/alma/atenciones-aph/{id}", atencion).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.derivaciones", hasSize(1)));
    }

    @Test
    @Transactional
    void validaConsentimientoDestinoContextoYEstado() throws Exception {
        String auth = bearer("derivacion9701@example.edu.co");
        String atencion = crearAtencion(auth);
        for (String destino : new String[]{"EA", "SPYB"}) {
            mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content(solicitud(atencion, destino, false, "null")))
                    .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("CONSENTIMIENTO_REQUERIDO"));
        }
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(solicitud(atencion, "EA", true, "null")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.peligroInminente").exists());
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(solicitud(atencion, "NO_EXISTE", false, CONSENTIMIENTO)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.destino").exists());
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(solicitud(atencion, "EA", false, CONSENTIMIENTO)
                                .replace("Motivo ficticio", "abc")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.motivo").exists());
        mockMvc.perform(get("/alma/derivaciones/internas").header("Authorization", auth)
                        .param("tipoContexto", "CASO").param("idContexto", atencion))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/alma/derivaciones/internas").header("Authorization", auth)
                        .param("tipoContexto", "ATENCION_APH").param("idContexto", "ALMA-2000-999999"))
                .andExpect(status().isNotFound());
        jdbc.update("UPDATE atencionalmaaph SET estado='CERRADO' WHERE codigo=?", atencion);
        entityManager.clear();
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(solicitud(atencion, "EA", false, CONSENTIMIENTO)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("ATENCION_NO_EDITABLE"));
    }

    @Test
    @Transactional
    void validaVersionYConsentimientoTambienEnEmergencia() throws Exception {
        String auth = bearer("derivacion9701@example.edu.co");
        String atencion = crearAtencion(auth);
        String conVersion = CONSENTIMIENTO.replace("\"VERBAL\"", "\"VERBAL\",\"versionTexto\":\"prueba-v1\"");
        String payload = solicitud(atencion, "SPYB", true, conVersion);
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['consentimiento.versionTexto']").exists());
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(payload.replace("2026-01-01T10:00:00Z", "2999-01-01T10:00:00Z")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['consentimiento.fechaRegistro']").exists());
        mockMvc.perform(post("/alma/derivaciones/internas").header("Authorization", auth)
                        .contentType(APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.consentimiento.versionTexto").value("prueba-v1"));
    }
}

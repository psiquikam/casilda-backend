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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
                new FileSystemResource("database/update_permisos_derivacion_psicologia.sql"));
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

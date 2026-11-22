package co.edu.udea.casilda.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.mock.mockito.MockBean;
import co.edu.udea.casilda.service.LimiteBandejaAphService;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BandejaAphIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @MockBean private LimiteBandejaAphService limiteBandeja;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        ResourceDatabasePopulator permisos = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_bandeja_aph.sql"));
        permisos.execute(dataSource);
        permisos.execute(dataSource);
        int rol = jdbc.queryForObject("SELECT id FROM rol WHERE codigo='TECNICO_APH'", Integer.class);
        for (int id : new int[]{9201, 9202}) {
            jdbc.update("""
                    INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                    SELECT ?, ?, password, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?
                    FROM usuario WHERE id=1
                    """, id, "tecnico" + id + "@example.edu.co", "Tecnico " + id, rol);
            jdbc.update("INSERT INTO usuariorol (idusuario,idrol) VALUES (?,?)", id, rol);
        }
    }

    private String solicitud(String origen, boolean peligro) {
        return """
                {"origenRemision":"%s","referenciaOrigen":"REF-1",
                 "persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"1000000001",
                            "primerNombre":"María del Carmen","segundoNombre":"Lucía",
                            "primerApellido":"De la Cruz","segundoApellido":"López","telefonoPrincipal":"3000000000"},
                 "motivo":"Remisión de prueba","peligroInminente":%s}
                """.formatted(origen, peligro);
    }

    @Test
    @Transactional
    void remisionSeTomaYSeListaSoloParaSuTecnico() throws Exception {
        String profesional = bearer("profesional@udea.edu.co");
        String tecnico = bearer("tecnico9201@example.edu.co");
        String otro = bearer("tecnico9202@example.edu.co");
        JsonNode creada = objectMapper.readTree(mockMvc.perform(post("/alma/remisiones-aph")
                        .header("Authorization", profesional).contentType(APPLICATION_JSON).content(solicitud("EA", true)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PENDIENTE_APH"))
                .andExpect(jsonPath("$.tomadaPor").doesNotExist()).andExpect(jsonPath("$.origenRemision").value("EA"))
                .andReturn().getResponse().getContentAsString());
        String id = creada.path("idAtencion").asText();
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content(solicitud("EA", true)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("REMISION_DUPLICADA"))
                .andExpect(jsonPath("$.detalles.idAtencion").value(id));
        mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", tecnico).param("origen", "EA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].persona.nombreCompleto").value("María del Carmen Lucía De la Cruz López"));
        mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", tecnico))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/bandeja/aph/{id}/tomar", id).header("Authorization", tecnico)
                        .contentType(APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_GESTION_APH"))
                .andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.tomadaPor").value("Tecnico 9201"));
        mockMvc.perform(post("/alma/bandeja/aph/{id}/tomar", id).header("Authorization", otro)
                        .contentType(APPLICATION_JSON).content("{\"version\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("REMISION_YA_TOMADA"));
        mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", tecnico).param("vista", "mias"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)));
        mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", otro).param("vista", "mias"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", id).header("Authorization", tecnico)
                        .contentType(APPLICATION_JSON).content("{\"version\":2,\"notaAph\":\"Nota\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM eventoatencionalma e JOIN atencionalmaaph a ON a.id=e.idatencion
                WHERE a.codigo=? AND e.accion='CAMBIO_ESTADO'
                """, Integer.class, id));
        assertEquals("true", jdbc.queryForObject(
                "SELECT datosremision->>'peligroInminente' FROM atencionalmaaph WHERE codigo=?", String.class, id));
        String detalle = jdbc.queryForObject("""
                SELECT e.detalle FROM eventoatencionalma e JOIN atencionalmaaph a ON a.id=e.idatencion
                WHERE a.codigo=? AND e.accion='CREACION'
                """, String.class, id);
        assertTrue(detalle.contains("excepción de consentimiento por peligro inminente"));
    }

    @Test
    @Transactional
    void verificaRolesFiltrosYConsentimiento() throws Exception {
        String profesional = bearer("profesional@udea.edu.co");
        String tecnico = bearer("tecnico9201@example.edu.co");
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content(solicitud("UAD", true)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", tecnico)
                        .contentType(APPLICATION_JSON).content(solicitud("EA", true)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content(solicitud("EA", false)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("CONSENTIMIENTO_REQUERIDO"));
        mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", tecnico).param("vista", "mias").param("origen", "EA"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/alma/bandeja/aph")).andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/bandeja/aph").header("Authorization", profesional))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.origenRemision").exists());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM atencionalmaaph", Integer.class));
    }

    @Test
    @Transactional
    void migracionPermiteRemisionesSinAsignarEnBasesExistentes() {
        jdbc.execute("ALTER TABLE atencionalmaaph ALTER COLUMN idusuariotomador SET NOT NULL");
        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_permisos_bandeja_aph.sql"));
        migration.execute(dataSource);
        migration.execute(dataSource);
        assertEquals("YES", jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema=current_schema() AND table_name='atencionalmaaph'
                  AND column_name='idusuariotomador'
                """, String.class));
    }

    @Test
    @Transactional
    void remisionConConsentimientoRegistraEvidenciaYSoloAceptaTextoActivo() throws Exception {
        String profesional = bearer("profesional@udea.edu.co");
        String consentimiento = """
                ,"consentimiento":{"otorgado":true,"medio":"VERBAL",
                  "fechaRegistro":"2026-01-01T10:00:00Z","versionTexto":"prueba-v1"}
                """;
        String request = solicitud("EA", false).replaceFirst("}\\s*$", consentimiento + "}");
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['consentimiento.versionTexto']").exists());
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','prueba-v1','Texto ficticio exclusivo de prueba',true)
                """);
        String body = mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).path("idAtencion").asText();
        assertEquals("prueba-v1", jdbc.queryForObject("""
                SELECT datosremision->'consentimiento'->>'versionTexto'
                FROM atencionalmaaph WHERE codigo=?
                """, String.class, id));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM atencionalmaaph
                WHERE codigo=? AND datosremision->>'fechaRecepcionConsentimiento' IS NOT NULL
                """, Integer.class, id));
        mockMvc.perform(post("/alma/bandeja/aph/{id}/tomar", id)
                        .header("Authorization", bearer("tecnico9201@example.edu.co"))
                        .contentType(APPLICATION_JSON).content("{\"version\":9}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
    }

    @Test
    @Transactional
    void rolDeOrigenNoPuedeSuplantarOtroModulo() throws Exception {
        for (String rol : new String[]{"EA_COORDINACION", "SPB_ANALISTA", "UAD_COORDINACION", "UAD_EQUIPO_3", "UAD_EQUIPO_4"}) {
            int id = 9300 + java.util.List.of("EA_COORDINACION", "SPB_ANALISTA", "UAD_COORDINACION",
                    "UAD_EQUIPO_3", "UAD_EQUIPO_4").indexOf(rol);
            jdbc.update("INSERT INTO rol (id,codigo,nombre,activo) VALUES (?,?,?,true)", id, rol, rol);
            jdbc.update("""
                    INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                    SELECT ?, ?, password, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?
                    FROM usuario WHERE id=1
                    """, id, rol.toLowerCase() + "@example.edu.co", rol, id);
            jdbc.update("INSERT INTO usuariorol (idusuario,idrol) VALUES (?,?)", id, id);
        }
        new ResourceDatabasePopulator(new FileSystemResource("database/update_permisos_bandeja_aph.sql"))
                .execute(dataSource);
        for (String rol : new String[]{"EA_COORDINACION", "SPB_ANALISTA", "UAD_COORDINACION", "UAD_EQUIPO_3", "UAD_EQUIPO_4"}) {
            String esperado = rol.startsWith("EA_") ? "EA" : rol.startsWith("SPB_") ? "SPYB" : "UAD";
            String auth = bearer(rol.toLowerCase() + "@example.edu.co");
            for (String origen : new String[]{"EA", "SPYB", "UAD"}) {
                mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", auth)
                                .contentType(APPLICATION_JSON).content(solicitud(origen, true)
                                        .replace("1000000001", "100000" + (9300 + java.util.List.of(
                                                "EA_COORDINACION", "SPB_ANALISTA", "UAD_COORDINACION",
                                                "UAD_EQUIPO_3", "UAD_EQUIPO_4").indexOf(rol)))))
                        .andExpect(status().is(origen.equals(esperado) ? 201 : 403));
            }
        }
    }

    @Test
    @Transactional
    void remisionExigeTelefonoYDetectaDuplicadosSegunOrigenYEstado() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        String valido = solicitud("EA", true);
        for (String telefono : new String[]{"", "123", "300 000 0000"}) {
            mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                            .contentType(APPLICATION_JSON).content(valido.replace("3000000000", telefono)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles['persona.telefonoPrincipal']").exists());
        }
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON)
                        .content(valido.replace(",\"telefonoPrincipal\":\"3000000000\"", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles['persona.telefonoPrincipal']").exists());
        String creada = mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(valido))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(creada).path("idAtencion").asText();
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(valido.replace("1000000001", "1000000002")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(valido.replace("\"CC\"", "\"TI\"")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(solicitud("UAD", true)))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE atencionalmaaph SET estado='EN_GESTION_APH' WHERE codigo=?", id);
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(valido))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.idAtencion").value(id));
        jdbc.update("UPDATE atencionalmaaph SET estado='CERRADO' WHERE codigo=?", id);
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(valido))
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void documentoAlfanumericoIgnoraMayusculasAlDetectarDuplicados() throws Exception {
        jdbc.update("INSERT INTO tipoidentificacion (id,codigo,nombre,activo) VALUES (9300,'CE','Extranjería',true)");
        String admin = bearer("admin@udea.edu.co");
        String solicitud = solicitud("EA", true).replace("\"CC\"", "\"CE\"")
                .replace("1000000001", "ab1234");
        String creada = mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(solicitud))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(creada).path("idAtencion").asText();
        mockMvc.perform(post("/alma/remisiones-aph").header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(solicitud.replace("ab1234", "AB1234")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("REMISION_DUPLICADA"))
                .andExpect(jsonPath("$.detalles.idAtencion").value(id));
    }
}

package co.edu.udea.casilda.integration;

import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import co.edu.udea.casilda.service.ConsultaPreviaAlmaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@TestPropertySource(properties = "app.alma.consulta-previa.habilitada=true")
class PrimerContactoPsicologiaIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;
    @Autowired private ConsultaPreviaAlmaService consultaPrevia;
    private static final String REQUEST = """
            {"version":4,"consentimiento":{"otorgado":true,"medio":"VERBAL",
             "fechaRegistro":"2026-01-01T10:00:00Z","versionTexto":"contacto-test"},
             "persona":{"primerNombre":"María del Carmen","primerApellido":"De la Cruz"},
             "valoracionRiesgo":{"nivel":"MEDIO","descripcion":"Descripción clínica privada"},
             "dimensiones":["DIM_01"],"acuerdos":"Acuerdos clínicos de prueba",
             "proximoControl":"2099-12-01T10:00:00Z"}
            """;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint','id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_derivacion_psicologia.sql"),
                new FileSystemResource("database/update_auditoria_recursos.sql"),
                new FileSystemResource("database/update_consulta_previa_aph.sql"),
                new FileSystemResource("database/update_permisos_gestion_psicologia.sql"),
                new FileSystemResource("database/migration_primer_contacto_psicologia.sql"),
                new FileSystemResource("database/insert_catalogos_primer_contacto_psicologia.sql"))
                .execute(dataSource);
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','contacto-test','Texto de prueba',true)
                ON CONFLICT (catalogo,codigo) DO UPDATE SET activo=true
                """);
    }

    private void novedad(String codigo, String documento, Long tomador) {
        jdbc.update("""
                INSERT INTO atencionalmaaph (codigo,version,estado,triaje,fechaderivacion,
                    consentimientomedio,consentimientofecha,consentimientoversion,
                    idusuariotomadorpsicologia,datos,fecha_creacion,fecha_actualizacion)
                VALUES (?,4,'PENDIENTE_PSICOLOGIA','MEDIO',now(),'VERBAL',now(),'contacto-test',?,
                    CAST(? AS jsonb),now(),now())
                """, codigo, tomador, """
                {"canalIngreso":"LLAMADA","persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"%s"}}
                """.formatted(documento));
    }

    private org.springframework.test.web.servlet.ResultActions registrar(String codigo, String token, String body)
            throws Exception {
        return mockMvc.perform(post("/alma/psicologia/novedades/{id}/primer-contacto", codigo)
                .header("Authorization", token).contentType(APPLICATION_JSON).content(body));
    }

    @Test
    void creaCasoGuardaContenidoClinicoSeparadoYActualizaConsulta() throws Exception {
        novedad("ALMA-2090-000001", "1234567890", 1L);
        String admin = bearer("admin@udea.edu.co");
        String body = registrar("ALMA-2090-000001", admin, REQUEST)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.vinculado").value(false))
                .andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.estadoAtencion").value("ABIERTO_ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String codigo = objectMapper.readTree(body).path("numeroCaso").asText();
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM primercontactopsicologia pc
                JOIN casopsicologia c ON c.id=pc.idcasopsicologia
                JOIN atencionalmaaph a ON a.id=pc.idatencion
                WHERE c.codigo=? AND c.prioridad='MEDIA' AND c.idpersona IS NOT NULL
                  AND pc.datos->'nota'->>'tipoOrigen'='PSICO'
                  AND pc.datos->'nota'->>'autor'='Admin'
                  AND pc.datos->'nota'->>'fechaRegistro' IS NOT NULL
                  AND a.datos->'valoracionRiesgo' IS NULL
                """, Integer.class, codigo));
        mockMvc.perform(get("/alma/psicologia/novedades/ALMA-2090-000001").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroCaso").value(codigo))
                .andExpect(jsonPath("$.valoracionRiesgo").doesNotExist());
        mockMvc.perform(get("/alma/atenciones-aph/ALMA-2090-000001").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroCaso").value(codigo))
                .andExpect(jsonPath("$.valoracionRiesgo").doesNotExist());
        var consulta = consultaPrevia.consultar("CC", "1234567890");
        org.junit.jupiter.api.Assertions.assertTrue(consulta.seguimientoActivo().activo());
        org.junit.jupiter.api.Assertions.assertNotNull(consulta.seguimientoActivo().proximoSeguimiento());
        org.junit.jupiter.api.Assertions.assertFalse(objectMapper.writeValueAsString(consulta).contains("clínica privada"));
        registrar("ALMA-2090-000001", admin, REQUEST).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("NOVEDAD_CERRADA"));
    }

    @Test
    void permiteCasoSinPersonaYExigeDecisionExplicitaParaRecontacto() throws Exception {
        novedad("ALMA-2090-000002", "1234567891", 1L);
        novedad("ALMA-2090-000003", "1234567891", 1L);
        String admin = bearer("admin@udea.edu.co");
        String sinNombres = REQUEST.replace("\"persona\":{\"primerNombre\":\"María del Carmen\",\"primerApellido\":\"De la Cruz\"},", "");
        String respuesta = registrar("ALMA-2090-000002", admin, sinNombres)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String codigo = objectMapper.readTree(respuesta).path("numeroCaso").asText();
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM casopsicologia WHERE codigo=? AND idpersona IS NULL",
                Integer.class, codigo));
        registrar("ALMA-2090-000003", admin, sinNombres).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CASO_ACTIVO_EXISTENTE"))
                .andExpect(jsonPath("$.detalles.numeroCaso").value(codigo));
        registrar("ALMA-2090-000003", admin, sinNombres.replace("\"version\":4",
                        "\"version\":4,\"vincularA\":\"" + codigo + "\""))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.numeroCaso").value(codigo))
                .andExpect(jsonPath("$.vinculado").value(true));
        assertEquals(2, jdbc.queryForObject("""
                SELECT count(*) FROM primercontactopsicologia pc
                JOIN casopsicologia c ON c.id=pc.idcasopsicologia WHERE c.codigo=?
                """, Integer.class, codigo));
    }

    @Test
    void exigePermisosPropiedadVersionYConsentimiento() throws Exception {
        novedad("ALMA-2090-000004", "1234567892", null);
        String admin = bearer("admin@udea.edu.co");
        registrar("ALMA-2090-000004", admin, REQUEST).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("NOVEDAD_NO_TOMADA"));
        jdbc.update("UPDATE atencionalmaaph SET idusuariotomadorpsicologia=1 WHERE codigo='ALMA-2090-000004'");
        entityManager.clear();
        registrar("ALMA-2090-000004", admin, REQUEST.replace("\"version\":4", "\"version\":3"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
        registrar("ALMA-2090-000004", bearer("revisor@udea.edu.co"), REQUEST).andExpect(status().isForbidden());
        registrar("ALMA-2090-000004", bearer("profesional@udea.edu.co"), REQUEST)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("NOVEDAD_NO_TOMADA"));
        registrar("ALMA-2090-000004", admin, REQUEST.replace("\"otorgado\":true", "\"otorgado\":false"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("CONSENTIMIENTO_REQUERIDO"));
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM primercontactopsicologia p JOIN atencionalmaaph a ON a.id=p.idatencion
                WHERE a.codigo='ALMA-2090-000004'
                """, Integer.class));
    }

    @Test
    void admiteCasoNuevoExplicitamenteAunqueExistaCasoActivo() throws Exception {
        novedad("ALMA-2090-000005", "1234567893", 1L);
        novedad("ALMA-2090-000006", "1234567893", 1L);
        String admin = bearer("admin@udea.edu.co");
        registrar("ALMA-2090-000005", admin, REQUEST).andExpect(status().isCreated());
        registrar("ALMA-2090-000006", admin, REQUEST.replace("\"version\":4",
                        "\"version\":4,\"vincularA\":\"CASO_NUEVO\""))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.vinculado").value(false));
        assertEquals(2, jdbc.queryForObject("""
                SELECT count(*) FROM casopsicologia WHERE numerodocumento='1234567893'
                """, Integer.class));
    }

    @Test
    void vinculaCasoExistenteYExponeElControlSinExponerLaNota() throws Exception {
        jdbc.update("INSERT INTO estadocaso (id,nombre) VALUES (9862,'Abierto activo')");
        jdbc.update("INSERT INTO estadocita (id,nombre) VALUES (9862,'Prueba')");
        jdbc.update("INSERT INTO regimen (id,nombre) VALUES (9862,'Prueba')");
        jdbc.update("INSERT INTO eps (id,nombre) VALUES (9862,'Prueba')");
        jdbc.update("INSERT INTO cita (id,idsolicitudatencion,fecha,idestadocita) VALUES (9862,100,now(),9862)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps,idestadocaso)
                VALUES (9862,'CAS-LEGACY-TEST',9862,1,9862,9862,9862)
                """);
        jdbc.update("UPDATE persona SET segundonombre='Lucía' WHERE id=101");
        novedad("ALMA-2090-000007", "100000002", 1L);
        String admin = bearer("admin@udea.edu.co");
        registrar("ALMA-2090-000007", admin, REQUEST)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CASO_ACTIVO_EXISTENTE"))
                .andExpect(jsonPath("$.detalles.numeroCaso").value("CAS-LEGACY-TEST"));
        registrar("ALMA-2090-000007", admin, REQUEST.replace("\"version\":4",
                        "\"version\":4,\"vincularA\":\"CAS-LEGACY-TEST\"")
                        .replace("\"primerNombre\":\"María del Carmen\"",
                                "\"segundoNombre\":null,\"primerNombre\":\"María del Carmen\""))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.vinculado").value(true))
                .andExpect(jsonPath("$.numeroCaso").value("CAS-LEGACY-TEST"));
        assertEquals("Lucía", jdbc.queryForObject("SELECT segundonombre FROM persona WHERE id=101", String.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM primercontactopsicologia WHERE idcaso=9862 AND idcasopsicologia IS NULL
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM eventocaso WHERE idcaso=9862 AND accion='VINCULACION'",
                Integer.class));
        var consulta = consultaPrevia.consultar("CC", "100000002");
        org.junit.jupiter.api.Assertions.assertTrue(consulta.seguimientoActivo().activo());
        assertEquals("2099-12-01T05:00-05:00", consulta.seguimientoActivo().proximoSeguimiento().toString());
        org.junit.jupiter.api.Assertions.assertFalse(objectMapper.writeValueAsString(consulta).contains("clínica privada"));
        novedad("ALMA-2090-000009", "100000002", 1L);
        registrar("ALMA-2090-000009", admin, REQUEST)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalles.proximoControl").isNotEmpty());
    }

    @Test
    void rechazaCasoDeOtraPersonaYDatosInvalidosSinEscrituras() throws Exception {
        novedad("ALMA-2090-000008", "1234567894", 1L);
        String admin = bearer("admin@udea.edu.co");
        registrar("ALMA-2090-000008", admin, REQUEST.replace("\"version\":4",
                        "\"version\":4,\"vincularA\":\"CAS-2090-000001\""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.vincularA").exists());
        registrar("ALMA-2090-000008", admin, REQUEST.replace("Descripción clínica privada", "corta"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles['valoracionRiesgo.descripcion']").exists());
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM primercontactopsicologia p JOIN atencionalmaaph a ON a.id=p.idatencion
                WHERE a.codigo='ALMA-2090-000008'
                """, Integer.class));
    }

    @Test
    void reconoceDocumentosAlfanumericosSinDistinguirMayusculas() throws Exception {
        jdbc.update("INSERT INTO tipoidentificacion (id,codigo,nombre,activo) VALUES (9863,'CE','Extranjería prueba',true)");
        novedad("ALMA-2090-000010", "123456", 1L);
        novedad("ALMA-2090-000011", "123456", 1L);
        jdbc.update("""
                UPDATE atencionalmaaph
                SET datos=jsonb_set(jsonb_set(datos,'{persona,tipoIdentificacion}','"CE"'::jsonb),
                    '{persona,numeroIdentificacion}', to_jsonb(CASE codigo
                        WHEN 'ALMA-2090-000010' THEN 'ab1234' ELSE 'AB1234' END))
                WHERE codigo IN ('ALMA-2090-000010','ALMA-2090-000011')
                """);
        String admin = bearer("admin@udea.edu.co");
        String respuesta = registrar("ALMA-2090-000010", admin, REQUEST)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String codigo = objectMapper.readTree(respuesta).path("numeroCaso").asText();
        registrar("ALMA-2090-000011", admin, REQUEST).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CASO_ACTIVO_EXISTENTE"))
                .andExpect(jsonPath("$.detalles.numeroCaso").value(codigo));
        org.junit.jupiter.api.Assertions.assertTrue(consultaPrevia.consultar("CE", "ab1234").seguimientoActivo().activo());
        mockMvc.perform(get("/alma/bandeja/psicologia").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].recontacto").value(true));
    }

    @Test
    void admiteRolesDeEscrituraYDeniegaOtrosRoles() throws Exception {
        novedad("ALMA-2090-000012", "1234567895", 2L);
        novedad("ALMA-2090-000013", "1234567896", 3L);
        registrar("ALMA-2090-000012", bearer("coordinador@udea.edu.co"), REQUEST)
                .andExpect(status().isCreated());
        registrar("ALMA-2090-000013", bearer("profesional@udea.edu.co"), REQUEST)
                .andExpect(status().isCreated());
        registrar("ALMA-2090-000013", bearer("user@udea.edu.co"), REQUEST).andExpect(status().isForbidden());
        jdbc.update("""
                INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                SELECT 9864,'tecnico.contacto@example.test',u.password,'Técnico de prueba',true,now(),now(),r.id
                FROM usuario u JOIN rol r ON r.codigo='TECNICO_APH' WHERE u.id=1
                """);
        jdbc.update("INSERT INTO usuariorol (idusuario,idrol) SELECT 9864,id FROM rol WHERE codigo='TECNICO_APH'");
        registrar("ALMA-2090-000013", bearer("tecnico.contacto@example.test"), REQUEST)
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/psicologia/novedades/ALMA-2090-000013/primer-contacto")
                        .contentType(APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isForbidden());
    }
}

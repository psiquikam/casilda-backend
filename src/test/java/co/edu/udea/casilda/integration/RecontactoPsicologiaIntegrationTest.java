package co.edu.udea.casilda.integration;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class RecontactoPsicologiaIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    private static final String ATENCION = "ALMA-2090-000001";

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint','id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_derivacion_psicologia.sql"),
                new FileSystemResource("database/update_auditoria_recursos.sql"),
                new FileSystemResource("database/update_permisos_gestion_psicologia.sql"),
                new FileSystemResource("database/migration_primer_contacto_psicologia.sql"),
                new FileSystemResource("database/insert_catalogos_primer_contacto_psicologia.sql"))
                .execute(dataSource);
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','contacto-test','Texto de prueba',true)
                ON CONFLICT (catalogo,codigo) DO UPDATE SET activo=true
                """);
        jdbc.update("""
                INSERT INTO atencionalmaaph (codigo,version,estado,triaje,fechaderivacion,
                    consentimientomedio,consentimientofecha,consentimientoversion,
                    idusuariotomador,idusuariotomadorpsicologia,datos,fecha_creacion,fecha_actualizacion)
                VALUES (?,4,'PENDIENTE_PSICOLOGIA','MEDIO',now(),'VERBAL',now(),'contacto-test',1,1,
                    '{"canalIngreso":"LLAMADA","persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"100000002"},
                      "motivoConsulta":"Motivo APH","notaAph":"Nota APH"}'::jsonb,now(),now())
                """, ATENCION);
        jdbc.update("""
                INSERT INTO casopsicologia (codigo,version,estado,prioridad,idtipoidentificacion,
                    numerodocumento,idpersona,idprofesional,fechaapertura,fechaactualizacion)
                VALUES ('CAS-2090-000001',1,'ABIERTO','MEDIA',1,'100000002',NULL,1,now(),now()),
                       ('CAS-2090-000002',1,'CERRADO','MEDIA',1,'100000002',NULL,1,now(),now())
                """);
    }

    private org.springframework.test.web.servlet.ResultActions resolver(String token, String cuerpo) throws Exception {
        return mockMvc.perform(post("/alma/psicologia/recontactos/{id}/resolucion", ATENCION)
                .header("Authorization", token).contentType(APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void listaCasosYProtegeRolesVersionYPropiedad() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        mockMvc.perform(get("/alma/psicologia/recontactos").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].casosDisponibles.length()").value(2))
                .andExpect(jsonPath("$[0].tomadaPorMi").value(true));
        mockMvc.perform(get("/alma/psicologia/recontactos").header("Authorization", bearer("revisor@udea.edu.co")))
                .andExpect(status().isOk());
        resolver(bearer("revisor@udea.edu.co"), "{}").andExpect(status().isForbidden());
        resolver(bearer("profesional@udea.edu.co"), """
                {"version":4,"decision":"CASO_NUEVO","nota":"Hechos distintos"}
                """).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("NOVEDAD_NO_TOMADA"));
        resolver(admin, """
                {"version":3,"decision":"CASO_NUEVO","nota":"Hechos distintos"}
                """).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
        resolver(admin, """
                {"version":4,"decision":"CONTENCION_PUNTUAL","numeroCaso":"CAS-2090-000002","nota":"Nota clínica"}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.estadoAtencion").exists());
        mockMvc.perform(post("/alma/psicologia/recontactos/ALMA-2090-999999/resolucion")
                .header("Authorization", admin).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NOVEDAD_NO_ENCONTRADA"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM resolucionrecontactopsicologia", Integer.class));
    }

    @Test
    void casoNuevoSaleDeRecontactosYPrimerContactoRespetaLaDecision() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        String cuerpo = """
                {"version":4,"decision":"CASO_NUEVO","nota":"Hechos distintos"}
                """;
        resolver(admin, cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.estadoAtencion").value("PENDIENTE_PSICOLOGIA"));
        resolver(admin, cuerpo).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RECONTACTO_YA_RESUELTO"));
        mockMvc.perform(get("/alma/psicologia/recontactos").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/alma/psicologia/notificaciones").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recontactosPendientes").value(0));
        mockMvc.perform(get("/alma/psicologia/novedades/{id}", ATENCION).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recontacto").value(false));
        String primerContacto = """
                {"version":5,"consentimiento":{"otorgado":true,"medio":"VERBAL",
                 "fechaRegistro":"2026-01-01T10:00:00Z","versionTexto":"contacto-test"},
                 "valoracionRiesgo":{"nivel":"MEDIO","descripcion":"Descripción clínica privada"},
                 "acuerdos":"Acuerdos clínicos de prueba","proximoControl":"2099-12-01T10:00:00Z"}
                """;
        mockMvc.perform(post("/alma/psicologia/novedades/{id}/primer-contacto", ATENCION)
                .header("Authorization", admin).contentType(APPLICATION_JSON)
                .content(primerContacto.replace("\"version\":5",
                        "\"version\":5,\"vincularA\":\"CAS-2090-000001\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.vincularA").exists());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM primercontactopsicologia", Integer.class));
        mockMvc.perform(post("/alma/psicologia/novedades/{id}/primer-contacto", ATENCION)
                .header("Authorization", admin).contentType(APPLICATION_JSON).content(primerContacto))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.vinculado").value(false));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM primercontactopsicologia", Integer.class));
    }

    @Test
    void contencionCerradaNoReabreNiExponeLaNotaAph() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        jdbc.update("""
                INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                SELECT 9864,'tecnico.recontacto@example.test',u.password,'Técnico de prueba',true,now(),now(),r.id
                FROM usuario u JOIN rol r ON r.codigo='TECNICO_APH' WHERE u.id=1
                """);
        jdbc.update("INSERT INTO usuariorol (idusuario,idrol) SELECT 9864,id FROM rol WHERE codigo='TECNICO_APH'");
        jdbc.update("UPDATE atencionalmaaph SET idusuariotomador=9864 WHERE codigo=?", ATENCION);
        resolver(admin, """
                {"version":4,"decision":"CONTENCION_PUNTUAL","numeroCaso":"CAS-2090-000002",
                 "nota":"Contenido clínico privado","estadoAtencion":"CERRADO","reabrirCaso":false}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.estadoAtencion").value("CERRADO"))
                .andExpect(jsonPath("$.numeroCaso").value("CAS-2090-000002"));
        assertEquals("CERRADO", jdbc.queryForObject(
                "SELECT estado FROM casopsicologia WHERE codigo='CAS-2090-000002'", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM primercontactopsicologia", Integer.class));
        mockMvc.perform(get("/alma/psicologia/recontactos/{id}/resolucion", ATENCION)
                .header("Authorization", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value("Contenido clínico privado"));
        mockMvc.perform(get("/alma/psicologia/novedades/{id}", ATENCION).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cierre.motivo").value("CONTENCION_PUNTUAL"))
                .andExpect(jsonPath("$.numeroCaso").value("CAS-2090-000002"));
        String tecnico = bearer("tecnico.recontacto@example.test");
        mockMvc.perform(get("/alma/psicologia/recontactos/{id}/resolucion", ATENCION)
                .header("Authorization", tecnico)).andExpect(status().isForbidden());
        String aph = mockMvc.perform(get("/alma/atenciones-aph/{id}", ATENCION).header("Authorization", tecnico))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(aph.contains("Contenido clínico privado"));
        assertFalse(aph.contains("CONTENCION_PUNTUAL"));
    }

    @Test
    void contencionPuedeContinuarYReabrirCasoPropioSinCrearSeguimiento() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        resolver(admin, """
                {"version":4,"decision":"CONTENCION_PUNTUAL","numeroCaso":"CAS-2090-000002",
                 "nota":"Contención clínica","estadoAtencion":"EN_SEGUIMIENTO","reabrirCaso":true}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.estadoAtencion").value("EN_SEGUIMIENTO"))
                .andExpect(jsonPath("$.reabrioCaso").value(true));
        assertEquals("ACTIVO", jdbc.queryForObject(
                "SELECT estado FROM casopsicologia WHERE codigo='CAS-2090-000002'", String.class));
        assertEquals(2, jdbc.queryForObject(
                "SELECT version FROM casopsicologia WHERE codigo='CAS-2090-000002'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM seguimientoatencion", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM primercontactopsicologia", Integer.class));
        mockMvc.perform(get("/alma/psicologia/novedades/{id}", ATENCION).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroCaso").value("CAS-2090-000002"));
    }

    @Test
    void rechazaCasoAjenoYReaperturaInvalidaSinEscrituras() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        resolver(admin, """
                {"version":4,"decision":"VINCULAR_A_CASO","numeroCaso":"CAS-2090-000002","nota":"Caso cerrado"}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.reabrirCaso").exists());
        resolver(admin, """
                {"version":4,"decision":"VINCULAR_A_CASO","numeroCaso":"CAS-2090-000001",
                 "nota":"Caso activo","reabrirCaso":true}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.reabrirCaso").exists());
        resolver(admin, """
                {"version":4,"decision":"VINCULAR_A_CASO","numeroCaso":"NO-EXISTE","nota":"Caso ajeno"}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.numeroCaso").exists());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM resolucionrecontactopsicologia", Integer.class));
        assertEquals(4, jdbc.queryForObject("SELECT version FROM atencionalmaaph WHERE codigo=?", Integer.class, ATENCION));
    }

    @Test
    void casoCerradoImpideCrearCasoSinDecisionExplicita() throws Exception {
        jdbc.update("DELETE FROM casopsicologia WHERE codigo='CAS-2090-000001'");
        String primerContacto = """
                {"version":4,"consentimiento":{"otorgado":true,"medio":"VERBAL",
                 "fechaRegistro":"2026-01-01T10:00:00Z","versionTexto":"contacto-test"},
                 "valoracionRiesgo":{"nivel":"MEDIO","descripcion":"Descripción clínica privada"},
                 "acuerdos":"Acuerdos clínicos de prueba","proximoControl":"2099-12-01T10:00:00Z"}
                """;
        mockMvc.perform(post("/alma/psicologia/novedades/{id}/primer-contacto", ATENCION)
                .header("Authorization", bearer("admin@udea.edu.co")).contentType(APPLICATION_JSON)
                .content(primerContacto)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RECONTACTO_PENDIENTE"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM primercontactopsicologia", Integer.class));
    }

    @Test
    void reabreCasoLegacyYConservaFechaDeCierre() throws Exception {
        jdbc.update("INSERT INTO estadocaso (id,nombre) VALUES (9862,'Cerrado'),(9863,'Abierto activo')");
        jdbc.update("INSERT INTO estadocita (id,nombre) VALUES (9862,'Prueba')");
        jdbc.update("INSERT INTO regimen (id,nombre) VALUES (9862,'Prueba')");
        jdbc.update("INSERT INTO eps (id,nombre) VALUES (9862,'Prueba')");
        jdbc.update("INSERT INTO cita (id,idsolicitudatencion,fecha,idestadocita) VALUES (9862,100,now(),9862)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps,idestadocaso,fechacierre)
                VALUES (9862,'CAS-LEGACY-TEST',9862,1,9862,9862,9862,'2026-01-01T00:00:00Z')
                """);
        String admin = bearer("admin@udea.edu.co");
        resolver(admin, """
                {"version":4,"decision":"VINCULAR_A_CASO","numeroCaso":"CAS-LEGACY-TEST",
                 "nota":"Caso anterior","reabrirCaso":true}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.reabrioCaso").value(true))
                .andExpect(jsonPath("$.estadoAtencion").value("EN_SEGUIMIENTO"));
        assertEquals("Abierto activo", jdbc.queryForObject("""
                SELECT ec.nombre FROM caso c JOIN estadocaso ec ON ec.id=c.idestadocaso WHERE c.id=9862
                """, String.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM resolucionrecontactopsicologia
                WHERE idcaso=9862 AND fechacierreanterior='2026-01-01T00:00:00Z'::timestamptz
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM eventocaso WHERE idcaso=9862 AND accion='REAPERTURA'
                """, Integer.class));
        mockMvc.perform(get("/alma/psicologia/novedades/{id}", ATENCION).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroCaso").value("CAS-LEGACY-TEST"));
    }
}

package co.edu.udea.casilda.integration;

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContencionPostCierreIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_contencion_post_cierre.sql"));
        scripts.execute(dataSource);
        scripts.execute(dataSource);
        jdbc.update("""
                INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                SELECT 8901,'contencion.aph@example.edu.co',u.password,'Técnico de prueba',true,CURRENT_TIMESTAMP,
                       CURRENT_TIMESTAMP,r.id FROM usuario u JOIN rol r ON r.codigo='TECNICO_APH' WHERE u.id=1
                """);
        jdbc.update("INSERT INTO usuariorol (idusuario,idrol) SELECT 8901,id FROM rol WHERE codigo='TECNICO_APH'");
        jdbc.update("""
                INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                SELECT 8902,'otro.aph@example.edu.co',u.password,'Otro técnico',true,CURRENT_TIMESTAMP,
                       CURRENT_TIMESTAMP,r.id FROM usuario u JOIN rol r ON r.codigo='TECNICO_APH' WHERE u.id=1
                """);
        jdbc.update("INSERT INTO usuariorol (idusuario,idrol) SELECT 8902,id FROM rol WHERE codigo='TECNICO_APH'");
        jdbc.update("INSERT INTO cargo (id,nombre) VALUES (8901,'Psicóloga')");
        jdbc.update("""
                INSERT INTO persona (id,primernombre,primerapellido,idtipoidentificacion,numerodocumento)
                VALUES (8901,'Profesional','Psicología',1,'890100001')
                """);
        jdbc.update("UPDATE usuario SET idpersona=8901 WHERE id=3");
        jdbc.update("INSERT INTO profesional (idpersona,idcargo) VALUES (8901,8901)");
        jdbc.update("""
                INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                SELECT 8903,'profesional.sin.cargo@example.edu.co',u.password,'Profesional sin cargo',true,
                       CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,r.id
                FROM usuario u JOIN rol r ON r.codigo='PROFESIONAL' WHERE u.id=1
                """);
        jdbc.update("INSERT INTO usuariorol (idusuario,idrol) SELECT 8903,id FROM rol WHERE codigo='PROFESIONAL'");
        jdbc.update("INSERT INTO estadocaso (id,nombre) VALUES (8901,'Cerrado'),(8902,'Abierto activo')");
        jdbc.update("INSERT INTO estadocita (id,nombre) VALUES (8901,'Prueba')");
        jdbc.update("INSERT INTO regimen (id,nombre) VALUES (8901,'Prueba')");
        jdbc.update("INSERT INTO eps (id,nombre) VALUES (8901,'Prueba')");
        jdbc.update("INSERT INTO cita (id,idsolicitudatencion,fecha,idestadocita) VALUES (8901,100,CURRENT_TIMESTAMP,8901)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps,idestadocaso,fechacreacion)
                VALUES (8901,'CAS-CERRADO',8901,1,8901,8901,8901,CURRENT_TIMESTAMP),
                       (8902,'CAS-ABIERTO',8901,1,8901,8901,8902,CURRENT_TIMESTAMP)
                """);
    }

    private String crearAtencion(String auth, String documento) throws Exception {
        String contenido = """
                {"canalIngreso":"LLAMADA",
                 "persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"%s"},
                 "motivoConsulta":"Motivo ficticio"}
                """.formatted(documento);
        return objectMapper.readTree(mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON).content(contenido))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("idAtencion").asText();
    }

    private String cuerpo(String atencion, String caso, String nota) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "idAtencion", atencion, "numeroCaso", caso, "nota", nota));
    }

    @Test
    void registraNotaYAuditoriaSinModificarEstadoNiVersion() throws Exception {
        String tecnico = bearer("contencion.aph@example.edu.co");
        String profesional = bearer("profesional@udea.edu.co");
        String id = crearAtencion(tecnico, "100000002");
        String nota = "Contenido sensible de prueba";
        mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", tecnico)
                        .contentType(APPLICATION_JSON).content(cuerpo(id, "CAS-CERRADO", nota)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.idContencion").isNotEmpty())
                .andExpect(jsonPath("$.idAtencion").value(id))
                .andExpect(jsonPath("$.numeroCaso").value("CAS-CERRADO"))
                .andExpect(jsonPath("$.tipoSeguimiento").value("POST_CIERRE"))
                .andExpect(jsonPath("$.nota").value(nota))
                .andExpect(jsonPath("$.autor").value("Técnico de prueba"));
        mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content(cuerpo(id, "CAS-CERRADO", "Otra nota de prueba")))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/alma/atenciones-aph/{id}", id).header("Authorization", tecnico))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.notas[0].tipoOrigen").value("POST_CIERRE"))
                .andExpect(jsonPath("$.notas[0].contenido").value(nota))
                .andExpect(jsonPath("$.notas[1].autor").value("Profesional"));
        assertEquals(8901, jdbc.queryForObject("SELECT idestadocaso FROM caso WHERE id=8901", Integer.class));
        assertEquals(2, jdbc.queryForObject("""
                SELECT count(*) FROM eventoatencionalma WHERE accion='NOTA'
                AND detalle='Se registró una nota de contención post-cierre.'
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM eventoatencionalma WHERE detalle LIKE '%sensible%'", Integer.class));
    }

    @Test
    void rechazaCasoAbiertoAjenoInexistenteYNotasInvalidas() throws Exception {
        String tecnico = bearer("contencion.aph@example.edu.co");
        String id = crearAtencion(tecnico, "100000002");
        String ajeno = crearAtencion(tecnico, "100000001");
        for (String caso : new String[]{"CAS-ABIERTO", "NO-EXISTE"}) {
            mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", tecnico)
                            .contentType(APPLICATION_JSON).content(cuerpo(id, caso, "Una nota válida")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PERSONA_SIN_CASO_CERRADO"));
        }
        mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", tecnico)
                        .contentType(APPLICATION_JSON).content(cuerpo(ajeno, "CAS-CERRADO", "Una nota válida")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("PERSONA_SIN_CASO_CERRADO"));
        for (String nota : new String[]{"corta", "x".repeat(2001)}) {
            mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", tecnico)
                            .contentType(APPLICATION_JSON).content(cuerpo(id, "CAS-CERRADO", nota)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.nota").exists());
        }
        mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", tecnico)
                        .contentType(APPLICATION_JSON).content(cuerpo("ALMA-2026-999999", "CAS-CERRADO", "Una nota válida")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", tecnico)
                        .contentType(APPLICATION_JSON).content(cuerpo(id, "", "Una nota válida")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.numeroCaso").exists());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM contencionpostcierre", Integer.class));
    }

    @Test
    void admiteLimitesDeNotaYPersonaDeSolicitudDirecta() throws Exception {
        jdbc.update("""
                INSERT INTO solicitudatencion (id, idsolicitante, ididentidadgenero, idtiposolicitud, idestadosolicitud)
                VALUES (8901, 100, 1, 1, 1)
                """);
        jdbc.update("INSERT INTO cita (id,idsolicitudatencion,fecha,idestadocita) VALUES (8902,8901,CURRENT_TIMESTAMP,8901)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps,idestadocaso,fechacreacion)
                VALUES (8903,'CAS-DIRECTO',8902,1,8901,8901,8901,CURRENT_TIMESTAMP)
                """);
        String tecnico = bearer("contencion.aph@example.edu.co");
        String id = crearAtencion(tecnico, "100000001");
        for (String nota : new String[]{"x".repeat(10), "x".repeat(2000)}) {
            mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", tecnico)
                            .contentType(APPLICATION_JSON).content(cuerpo(id, "CAS-DIRECTO", nota)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.nota").value(nota));
        }
    }

    @Test
    void soloRolesAutorizadosPuedenRegistrar() throws Exception {
        String tecnico = bearer("contencion.aph@example.edu.co");
        String id = crearAtencion(tecnico, "100000002");
        String contenido = cuerpo(id, "CAS-CERRADO", "Una nota válida");
        mockMvc.perform(post("/alma/contenciones-post-cierre").contentType(APPLICATION_JSON).content(contenido))
                .andExpect(status().isForbidden());
        for (String email : new String[]{"admin@udea.edu.co", "revisor@udea.edu.co", "coordinador@udea.edu.co"}) {
            mockMvc.perform(post("/alma/contenciones-post-cierre").header("Authorization", bearer(email))
                            .contentType(APPLICATION_JSON).content(contenido))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/alma/contenciones-post-cierre")
                        .header("Authorization", bearer("profesional.sin.cargo@example.edu.co"))
                        .contentType(APPLICATION_JSON).content(contenido))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/alma/contenciones-post-cierre")
                        .header("Authorization", bearer("otro.aph@example.edu.co"))
                        .contentType(APPLICATION_JSON).content(contenido))
                .andExpect(status().isForbidden());
    }
}

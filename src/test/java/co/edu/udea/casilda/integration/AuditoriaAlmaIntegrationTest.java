package co.edu.udea.casilda.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.alma.consulta-previa.habilitada=true")
class AuditoriaAlmaIntegrationTest extends IntegrationTestBase {
    private static final AtomicLong DOCUMENTO = new AtomicLong(1_000_000_001L);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    private final List<String> atencionesCreadas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        atencionesCreadas.clear();
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_historial_auditoria_aph.sql"),
                new FileSystemResource("database/update_consulta_previa_aph.sql"))
                .execute(dataSource);
        jdbc.update("DELETE FROM limiteauditoriaaph WHERE idusuario IN (1,4,8710)");
        Integer rolId = jdbc.queryForObject("SELECT id FROM rol WHERE codigo='ALMA_COORDINACION'", Integer.class);
        jdbc.update("""
                INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                SELECT 8710,'coordinacion.alma@example.edu.co',u.password,'Coordinación Alma',true,
                       CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,? FROM usuario u WHERE u.id=1
                """, rolId);
        jdbc.update("INSERT INTO usuariorol (idusuario,idrol) VALUES (8710,?)", rolId);
        jdbc.update("INSERT INTO estadocaso (id,nombre) VALUES (8910,'Abierto activo'),(8911,'Cerrado')");
        jdbc.update("INSERT INTO estadocita (id,nombre) VALUES (8910,'Prueba')");
        jdbc.update("INSERT INTO regimen (id,nombre) VALUES (8910,'Prueba')");
        jdbc.update("INSERT INTO eps (id,nombre) VALUES (8910,'Prueba')");
        jdbc.update("INSERT INTO cita (id,idsolicitudatencion,fecha,idestadocita) VALUES (8910,100,CURRENT_TIMESTAMP,8910)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps,idestadocaso)
                VALUES (8910,'CAS-AUD-PRUEBA',8910,1,8910,8910,8910)
                """);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM limiteauditoriaaph WHERE idusuario IN (1,4,8710)");
        jdbc.update("DELETE FROM usuariorol WHERE idusuario=8710");
        jdbc.update("DELETE FROM usuario WHERE id=8710");
        jdbc.update("DELETE FROM eventocaso WHERE idcaso=8910");
        jdbc.update("DELETE FROM caso WHERE id=8910");
        jdbc.update("DELETE FROM cita WHERE id=8910");
        jdbc.update("DELETE FROM eps WHERE id=8910");
        jdbc.update("DELETE FROM regimen WHERE id=8910");
        jdbc.update("DELETE FROM estadocita WHERE id=8910");
        jdbc.update("DELETE FROM estadocaso WHERE id IN (8910,8911)");
        for (String codigo : atencionesCreadas) {
            jdbc.update("DELETE FROM eventoatencionalma WHERE idatencion=(SELECT id FROM atencionalmaaph WHERE codigo=?)",
                    codigo);
            jdbc.update("DELETE FROM atencionalmaaph WHERE codigo=?", codigo);
        }
    }

    private String crearAtencion(String token) throws Exception {
        String documento = Long.toString(DOCUMENTO.getAndIncrement());
        String response = mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", token).contentType(APPLICATION_JSON).content("""
                                {"canalIngreso":"LLAMADA",
                                 "persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"%s"},
                                 "motivoConsulta":"Motivo de auditoría"}
                                """.formatted(documento)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String codigo = objectMapper.readTree(response).path("idAtencion").asText();
        atencionesCreadas.add(codigo);
        return codigo;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder auditoria(
            String codigo, String token) {
        return get("/alma/auditoria").param("tipoContexto", "ATENCION_APH")
                .param("idContexto", codigo).header("Authorization", token);
    }

    @Test
    void devuelveEventosCronologicosConDetalleSeguro() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        String codigo = crearAtencion(admin);
        mockMvc.perform(patch("/alma/atenciones-aph/{id}", codigo).header("Authorization", admin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"version\":1,\"notaAph\":\"Nota clínica que no debe aparecer en auditoría\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(auditoria(codigo, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].idEvento", containsString("EVT-")))
                .andExpect(jsonPath("$[0].usuario").value("Admin"))
                .andExpect(jsonPath("$[0].accion").value("CREACION"))
                .andExpect(jsonPath("$[0].detalle").value("Se creó la atención APH en estado borrador."))
                .andExpect(jsonPath("$[1].accion").value("ACTUALIZACION"))
                .andExpect(jsonPath("$[1].detalle").value("Se actualizaron datos de la atención APH."))
                .andExpect(jsonPath("$[1].detalle", not(containsString("Nota clínica"))));
    }

    @Test
    void permiteSoloAdminYCoordinacionAlmaYExigeContexto() throws Exception {
        String codigo = crearAtencion(bearer("admin@udea.edu.co"));
        String coordinacion = bearer("coordinacion.alma@example.edu.co");
        mockMvc.perform(auditoria(codigo, coordinacion)).andExpect(status().isOk());
        mockMvc.perform(auditoria(codigo, bearer("revisor@udea.edu.co"))).andExpect(status().isForbidden());
        mockMvc.perform(auditoria(codigo, bearer("user@udea.edu.co"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/alma/auditoria").header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.tipoContexto").exists());
        mockMvc.perform(get("/alma/auditoria").param("tipoContexto", "ATENCION_APH")
                        .param("idContexto", "ALMA-2099-999999")
                        .header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultaAuditoriaDeCasoPorCodigoEIdSinExponerDetallePrivado() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        mockMvc.perform(post("/atenciones/pestana/11").header("Authorization", admin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"idCaso\":8910,\"idEstadoCaso\":8911}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/alma/auditoria").param("tipoContexto", "CASO")
                        .param("idContexto", "CAS-AUD-PRUEBA").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("CAMBIO_ESTADO"))
                .andExpect(jsonPath("$[0].usuario").value("Admin"))
                .andExpect(jsonPath("$[0].detalle").value("Se cambió el estado del caso."))
                .andExpect(jsonPath("$[0].detalle", not(containsString("Cerrado"))));

        mockMvc.perform(get("/alma/auditoria").param("tipoContexto", "CASO")
                        .param("idContexto", "8910").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEvento", containsString("EVT-")));
    }

    @Test
    void limitaConsultasDeAuditoriaPorUsuario() throws Exception {
        String admin = bearer("admin@udea.edu.co");
        String codigo = crearAtencion(admin);
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(auditoria(codigo, admin)).andExpect(status().isOk());
        }
        mockMvc.perform(auditoria(codigo, admin))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("LIMITE_CONSULTAS"));
    }
}

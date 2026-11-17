package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BandejaAphConcurrencyIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @Test
    void soloUnaTomaSimultaneaTieneExito() throws Exception {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_permisos_bandeja_aph.sql")).execute(dataSource);
        int rol = jdbc.queryForObject("SELECT id FROM rol WHERE codigo='TECNICO_APH'", Integer.class);
        for (int id : new int[]{9401, 9402}) {
            jdbc.update("""
                    INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                    SELECT ?, ?, password, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?
                    FROM usuario WHERE id=1
                    """, id, "concurrente" + id + "@example.edu.co", "Tecnico " + id, rol);
            jdbc.update("INSERT INTO usuariorol (idusuario,idrol) VALUES (?,?)", id, rol);
        }
        String codigo = null;
        var executor = Executors.newFixedThreadPool(2);
        try {
            String cuerpo = mockMvc.perform(post("/alma/remisiones-aph")
                            .header("Authorization", bearer("admin@udea.edu.co")).contentType(APPLICATION_JSON)
                            .content("""
                                    {"origenRemision":"EA","persona":{"tipoIdentificacion":"CC",
                                    "numeroIdentificacion":"1000000001"},"motivo":"Remisión de prueba",
                                    "peligroInminente":true}
                                    """))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            codigo = objectMapper.readTree(cuerpo).path("idAtencion").asText();
            String id = codigo;
            List<String> auths = List.of(bearer("concurrente9401@example.edu.co"),
                    bearer("concurrente9402@example.edu.co"));
            CountDownLatch listos = new CountDownLatch(2);
            CountDownLatch inicio = new CountDownLatch(1);
            var tareas = auths.stream().map(auth -> executor.submit(() -> {
                listos.countDown();
                if (!inicio.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se pudo iniciar la prueba concurrente");
                }
                return mockMvc.perform(post("/alma/bandeja/aph/{id}/tomar", id)
                                .header("Authorization", auth).contentType(APPLICATION_JSON)
                                .content("{\"version\":1}"))
                        .andReturn().getResponse();
            })).toList();
            assertTrue(listos.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            var respuestas = List.of(tareas.get(0).get(20, TimeUnit.SECONDS), tareas.get(1).get(20, TimeUnit.SECONDS));
            assertEquals(List.of(200, 409), respuestas.stream().map(r -> r.getStatus()).sorted().toList());
            String conflicto = respuestas.stream().filter(r -> r.getStatus() == 409).findFirst().orElseThrow()
                    .getContentAsString();
            assertEquals("REMISION_YA_TOMADA", objectMapper.readTree(conflicto).path("codigo").asText());
            assertEquals(1, jdbc.queryForObject("""
                    SELECT count(*) FROM eventoatencionalma e JOIN atencionalmaaph a ON a.id=e.idatencion
                    WHERE a.codigo=? AND e.accion='CAMBIO_ESTADO'
                    """, Integer.class, id));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
            if (codigo != null) {
                jdbc.update("""
                        DELETE FROM eventoatencionalma WHERE idatencion IN
                        (SELECT id FROM atencionalmaaph WHERE codigo=?)
                        """, codigo);
                jdbc.update("DELETE FROM atencionalmaaph WHERE codigo=?", codigo);
            }
            jdbc.update("DELETE FROM usuariorol WHERE idusuario IN (9401,9402)");
            jdbc.update("DELETE FROM usuario WHERE id IN (9401,9402)");
        }
    }
}

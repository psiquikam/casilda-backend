package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AtencionAlmaConcurrencyIntegrationTest extends IntegrationTestBase {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    void dosActualizacionesConLaMismaVersionNoSobrescribenDatos() throws Exception {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"))
                .execute(dataSource);
        String auth = bearer("admin@udea.edu.co");
        String cuerpo = mockMvc.perform(post("/alma/atenciones-aph")
                        .header("Authorization", auth).contentType(APPLICATION_JSON)
                        .content("""
                                {"canalIngreso":"LLAMADA",
                                 "persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"1000000001"}}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String codigo = objectMapper.readTree(cuerpo).path("idAtencion").asText();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch listo = new CountDownLatch(2);
            CountDownLatch iniciar = new CountDownLatch(1);
            var operacion = (java.util.concurrent.Callable<Integer>) () -> {
                listo.countDown();
                if (!iniciar.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se pudo iniciar la prueba concurrente");
                }
                return mockMvc.perform(patch("/alma/atenciones-aph/{id}", codigo)
                                .header("Authorization", auth).contentType(APPLICATION_JSON)
                                .content("{\"version\":1,\"motivoConsulta\":\"Actualización\"}"))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> primero = executor.submit(operacion);
            Future<Integer> segundo = executor.submit(operacion);
            if (!listo.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Las peticiones concurrentes no están listas");
            }
            iniciar.countDown();
            assertEquals(List.of(200, 409), java.util.stream.Stream.of(
                    primero.get(20, TimeUnit.SECONDS), segundo.get(20, TimeUnit.SECONDS)).sorted().toList());
            assertEquals(2, jdbc.queryForObject(
                    "SELECT version FROM atencionalmaaph WHERE codigo = ?", Integer.class, codigo));
            assertEquals(2, jdbc.queryForObject("""
                    SELECT count(*) FROM eventoatencionalma e
                    JOIN atencionalmaaph a ON a.id=e.idatencion WHERE a.codigo=?
                    """, Integer.class, codigo));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
            jdbc.update("""
                    DELETE FROM eventoatencionalma WHERE idatencion IN
                    (SELECT id FROM atencionalmaaph WHERE codigo=?)
                    """, codigo);
            jdbc.update("DELETE FROM atencionalmaaph WHERE codigo=?", codigo);
        }
    }

    @Test
    void dosTriajesSimultaneosSoloPermitenUnaEscritura() throws Exception {
            jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
            new ResourceDatabasePopulator(
                    new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                    new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                    new FileSystemResource("database/update_permisos_triaje_alma_aph.sql")).execute(dataSource);
            jdbc.update("""
                    INSERT INTO usuario (id, email, password, nombre, activo, fechacreacion, fechaactualizacion, idrol)
                    SELECT 9010, 'triaje-concurrente@example.edu.co', password, 'Prueba APH', true,
                           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, (SELECT id FROM rol WHERE codigo='TECNICO_APH')
                    FROM usuario WHERE id=1
                    """);
            jdbc.update("""
                    INSERT INTO usuariorol (idusuario, idrol)
                    SELECT 9010, id FROM rol WHERE codigo='TECNICO_APH'
                    """);
            String codigo = null;
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                String auth = bearer("triaje-concurrente@example.edu.co");
                String cuerpo = mockMvc.perform(post("/alma/atenciones-aph")
                                .header("Authorization", auth).contentType(APPLICATION_JSON)
                                .content("""
                                        {"canalIngreso":"LLAMADA","motivoConsulta":"Prueba",
                                         "persona":{"tipoIdentificacion":"CC","numeroIdentificacion":"1000000001"}}
                                        """))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
                codigo = objectMapper.readTree(cuerpo).path("idAtencion").asText();
                String id = codigo;
                CountDownLatch listo = new CountDownLatch(2);
                CountDownLatch iniciar = new CountDownLatch(1);
                var operacion = (java.util.concurrent.Callable<Integer>) () -> {
                    listo.countDown();
                    if (!iniciar.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No se pudo iniciar la prueba concurrente");
                    }
                    return mockMvc.perform(put("/alma/atenciones-aph/{id}/triaje", id)
                                    .header("Authorization", auth).contentType(APPLICATION_JSON)
                                    .content("{\"version\":1,\"triaje\":\"MEDIO\"}"))
                            .andReturn().getResponse().getStatus();
                };
                Future<Integer> primero = executor.submit(operacion);
                Future<Integer> segundo = executor.submit(operacion);
                if (!listo.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Las peticiones concurrentes no están listas");
                }
                iniciar.countDown();
                assertEquals(List.of(200, 409), java.util.stream.Stream.of(
                        primero.get(20, TimeUnit.SECONDS), segundo.get(20, TimeUnit.SECONDS)).sorted().toList());
                assertEquals(2, jdbc.queryForObject(
                        "SELECT version FROM atencionalmaaph WHERE codigo=?", Integer.class, id));
                assertEquals(1, jdbc.queryForObject("""
                        SELECT count(*) FROM eventoatencionalma e
                        JOIN atencionalmaaph a ON a.id=e.idatencion
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
                jdbc.update("DELETE FROM usuariorol WHERE idusuario=9010");
                jdbc.update("DELETE FROM usuario WHERE id=9010");
        }
    }
}

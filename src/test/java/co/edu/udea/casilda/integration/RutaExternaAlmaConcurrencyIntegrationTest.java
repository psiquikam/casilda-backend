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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RutaExternaAlmaConcurrencyIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @Test
    void dosActualizacionesConLaMismaVersionNoSobrescribenLaRuta() throws Exception {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_rutas_externas_aph.sql"),
                new FileSystemResource("database/insert_catalogos_aph.sql")).execute(dataSource);
        String auth = bearer("admin@udea.edu.co");
        String atencion = null;
        var executor = Executors.newFixedThreadPool(2);
        try {
            String cuerpo = mockMvc.perform(post("/alma/atenciones-aph").header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content("""
                                    {"canalIngreso":"LLAMADA","persona":{"tipoIdentificacion":"CC",
                                    "numeroIdentificacion":"1000000001"}}
                                    """))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            atencion = objectMapper.readTree(cuerpo).path("idAtencion").asText();
            String rutaBody = mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth)
                            .contentType(APPLICATION_JSON).content("""
                                    {"tipoContexto":"ATENCION_APH","idContexto":"%s",
                                     "entidad":"LINEA_123","motivo":"Motivo ficticio",
                                     "fechaActivacion":"2026-11-17","peligroInminente":true}
                                    """.formatted(atencion)))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String idRuta = objectMapper.readTree(rutaBody).path("idRuta").asText();
            CountDownLatch listos = new CountDownLatch(2);
            CountDownLatch inicio = new CountDownLatch(1);
            var tareas = List.of("EN_SEGUIMIENTO", "CERRADA").stream().map(estado -> executor.submit(() -> {
                listos.countDown();
                if (!inicio.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se pudo iniciar la prueba concurrente");
                }
                return mockMvc.perform(patch("/alma/rutas-externas/{id}", idRuta).header("Authorization", auth)
                                .contentType(APPLICATION_JSON).content("""
                                        {"version":1,"estado":"%s","resultado":"ATENDIDA"}
                                        """.formatted(estado)))
                        .andReturn().getResponse().getStatus();
            })).toList();
            assertTrue(listos.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            assertEquals(List.of(200, 409), List.of(tareas.get(0).get(20, TimeUnit.SECONDS),
                    tareas.get(1).get(20, TimeUnit.SECONDS)).stream().sorted().toList());
            assertEquals(2, jdbc.queryForObject("SELECT version FROM rutaexternaalma WHERE id=?",
                    Integer.class, Long.parseLong(idRuta.substring(4))));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
            if (atencion != null) {
                jdbc.update("DELETE FROM rutaexternaalma WHERE idatencion IN (SELECT id FROM atencionalmaaph WHERE codigo=?)",
                        atencion);
                jdbc.update("DELETE FROM eventoatencionalma WHERE idatencion IN (SELECT id FROM atencionalmaaph WHERE codigo=?)",
                        atencion);
                jdbc.update("DELETE FROM atencionalmaaph WHERE codigo=?", atencion);
            }
        }
    }
}

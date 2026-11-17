package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CierreAlmaAphConcurrencyIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @Test
    void cierreYActivacionConcurrentesNoDejanRutasAbiertasEnAtencionCerrada() throws Exception {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        new ResourceDatabasePopulator(
                new FileSystemResource("database/update_permisos_borrador_alma_aph.sql"),
                new FileSystemResource("database/update_permisos_rutas_externas_aph.sql"),
                new FileSystemResource("database/update_permisos_cierre_alma_aph.sql"),
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
            jdbc.update("UPDATE atencionalmaaph SET estado='EN_GESTION_APH' WHERE codigo=?", atencion);
            String codigo = atencion;
            CountDownLatch listos = new CountDownLatch(2);
            CountDownLatch inicio = new CountDownLatch(1);
            var cierre = executor.submit(() -> {
                listos.countDown();
                if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Inicio concurrente fallido");
                return mockMvc.perform(post("/alma/atenciones-aph/{id}/cierre", codigo)
                                .header("Authorization", auth).contentType(APPLICATION_JSON)
                                .content("{\"version\":1,\"motivo\":\"ORIENTACION_BRINDADA\"}"))
                        .andReturn().getResponse().getStatus();
            });
            var ruta = executor.submit(() -> {
                listos.countDown();
                if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Inicio concurrente fallido");
                return mockMvc.perform(post("/alma/rutas-externas").header("Authorization", auth)
                                .contentType(APPLICATION_JSON).content("""
                                        {"tipoContexto":"ATENCION_APH","idContexto":"%s","entidad":"LINEA_123",
                                         "motivo":"Motivo ficticio","fechaActivacion":"2026-11-17",
                                         "peligroInminente":true}
                                        """.formatted(codigo)))
                        .andReturn().getResponse().getStatus();
            });
            assertTrue(listos.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            int estadoCierre = cierre.get(20, TimeUnit.SECONDS);
            int estadoRuta = ruta.get(20, TimeUnit.SECONDS);
            assertTrue((estadoCierre == 200 && estadoRuta == 422)
                    || (estadoCierre == 422 && estadoRuta == 201));
            assertEquals(0, jdbc.queryForObject("""
                    SELECT count(*) FROM rutaexternaalma r JOIN atencionalmaaph a ON a.id=r.idatencion
                    WHERE a.codigo=? AND a.estado='CERRADO' AND r.estado<>'CERRADA'
                    """, Integer.class, codigo));
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

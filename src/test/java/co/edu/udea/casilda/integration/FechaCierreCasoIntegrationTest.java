package co.edu.udea.casilda.integration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FechaCierreCasoIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void preparar() {
        ResourceDatabasePopulator migracion = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_fecha_cierre_caso.sql"));
        migracion.execute(dataSource);
        migracion.execute(dataSource);
        jdbc.update("INSERT INTO estadocaso (id,nombre) VALUES (8901,'Abierto activo'),(8902,'Cerrado')");
        jdbc.update("INSERT INTO estadocita (id,nombre) VALUES (8901,'Prueba')");
        jdbc.update("INSERT INTO regimen (id,nombre) VALUES (8901,'Prueba')");
        jdbc.update("INSERT INTO eps (id,nombre) VALUES (8901,'Prueba')");
        jdbc.update("INSERT INTO cita (id,idsolicitudatencion,fecha,idestadocita) VALUES (8901,100,CURRENT_TIMESTAMP,8901)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps,idestadocaso)
                VALUES (8901,'CAS-CIERRE-PRUEBA',8901,1,8901,8901,8901)
                """);
    }

    private void estado(String token, int estado) throws Exception {
        mockMvc.perform(post("/atenciones/pestana/11").header("Authorization", token)
                        .contentType(APPLICATION_JSON).content("{\"idCaso\":8901,\"idEstadoCaso\":" + estado + "}"))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
    }

    private Instant fecha() {
        var timestamp = jdbc.queryForObject("SELECT fechacierre FROM caso WHERE id=8901", java.sql.Timestamp.class);
        return timestamp == null ? null : timestamp.toInstant();
    }

    @Test
    @Transactional
    void cierraCorrigeReabreYVuelveACerrar() throws Exception {
        String profesional = bearer("profesional@udea.edu.co");
        String coordinador = bearer("coordinador@udea.edu.co");
        String ruta = "/solicitudes/acompanamiento/100/casos/8901/fecha-cierre";
        String correccion = "{\"fechaCierre\":\"2024-01-02T03:04:05Z\"}";
        assertNull(fecha());
        mockMvc.perform(patch(ruta).header("Authorization", coordinador)
                        .contentType(APPLICATION_JSON).content(correccion))
                .andExpect(status().isBadRequest());
        estado(profesional, 8902);
        assertNotNull(fecha());
        mockMvc.perform(patch(ruta).header("Authorization", coordinador)
                        .contentType(APPLICATION_JSON).content(correccion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fechaCierre").value("2024-01-02T03:04:05Z"));
        entityManager.flush();
        entityManager.clear();
        assertEquals(Instant.parse("2024-01-02T03:04:05Z"), fecha());
        mockMvc.perform(get("/casos/paginado").header("Authorization", coordinador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fechaCierre").value("2024-01-02T03:04:05Z"));
        estado(profesional, 8902);
        assertEquals(Instant.parse("2024-01-02T03:04:05Z"), fecha());
        estado(profesional, 8901);
        assertNull(fecha());
        estado(profesional, 8902);
        assertNotNull(fecha());
    }

    @Test
    @Transactional
    void restringeCorreccionAlPermisoDeAsignarYALaSolicitudCorrespondiente() throws Exception {
        String ruta = "/solicitudes/acompanamiento/100/casos/8901/fecha-cierre";
        String correccion = "{\"fechaCierre\":\"2024-01-02T03:04:05Z\"}";
        estado(bearer("profesional@udea.edu.co"), 8902);
        mockMvc.perform(patch(ruta).contentType(APPLICATION_JSON).content(correccion))
                .andExpect(status().isForbidden());
        for (String email : new String[]{"profesional@udea.edu.co", "revisor@udea.edu.co", "user@udea.edu.co"}) {
            mockMvc.perform(patch(ruta).header("Authorization", bearer(email))
                            .contentType(APPLICATION_JSON).content(correccion))
                    .andExpect(status().isForbidden());
        }
        String admin = bearer("admin@udea.edu.co");
        mockMvc.perform(patch("/solicitudes/acompanamiento/999/casos/8901/fecha-cierre")
                        .header("Authorization", admin).contentType(APPLICATION_JSON).content(correccion))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(ruta).header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(ruta).header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content("{\"fechaCierre\":\"no-es-fecha\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(ruta).header("Authorization", admin)
                        .contentType(APPLICATION_JSON).content(correccion)).andExpect(status().isOk());
    }

    @Test
    @Transactional
    void cambioDePermisosDeAsignacionSeAplicaInmediatamente() throws Exception {
        String profesional = bearer("profesional@udea.edu.co");
        estado(profesional, 8902);
        String ruta = "/solicitudes/acompanamiento/100/casos/8901/fecha-cierre";
        String correccion = "{\"fechaCierre\":\"2024-01-02T03:04:05Z\"}";
        jdbc.update("INSERT INTO endpointrole (idendpoint,idrol) VALUES (11,3)");
        mockMvc.perform(patch(ruta).header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content(correccion)).andExpect(status().isOk());
        jdbc.update("DELETE FROM endpointrole WHERE idendpoint=11 AND idrol=3");
        entityManager.clear();
        mockMvc.perform(patch(ruta).header("Authorization", profesional)
                        .contentType(APPLICATION_JSON).content(correccion)).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void casoHistoricoNoAdquiereFechaAlGuardarEstadoCerradoSinTransicion() throws Exception {
        jdbc.update("UPDATE caso SET idestadocaso=8902 WHERE id=8901");
        estado(bearer("profesional@udea.edu.co"), 8902);
        assertNull(fecha());
    }
}

package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogosServicioIntegrationTest extends IntegrationTestBase {

    @Test
    void tiposServicioIncluyeTrabajoSocial() throws Exception {
        mockMvc.perform(get("/maestros/tipos-servicio")
                        .header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Trabajo Social")));
    }

    @Test
    void lineaAlmaGuardaServicioSeleccionadoYRechazaDesconocido() throws Exception {
        String body = """
                {"idTipoReporte":1,"idCanalContacto":1,"fechaHoraAtencion":"2026-11-09T12:00:00",
                 "idPersonaAtiende":8,"idTipoServicio":%d,"idPersonaRegistra":1}
                """;
        String token = bearer("admin@udea.edu.co");
        mockMvc.perform(post("/linea-alma/registros/pestana/0")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON).content(body.formatted(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTipoServicio").value(5));

        mockMvc.perform(post("/linea-alma/registros/pestana/0")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON).content(body.formatted(999)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/linea-alma/registros/pestana/0")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(body.formatted(5).replace("\"idTipoServicio\":5,", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.idTipoServicio").exists());
    }
}

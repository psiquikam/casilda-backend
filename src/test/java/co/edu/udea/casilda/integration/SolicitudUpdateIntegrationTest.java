package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitudUpdateIntegrationTest extends IntegrationTestBase {

    @Test
    void updatesExistingSeededSolicitud() throws Exception {
        String token = login("admin@udea.edu.co");

        mockMvc.perform(put("/solicitudes/acompanamiento/100")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "primerNombre":"Ana María",
                                  "primerApellido":"Gómez López",
                                  "tipoDocumentoId":1,
                                  "numeroDocumento":"100000001",
                                  "identidadGeneroId":1,
                                  "correos":[{"correo":"ana.updated@example.edu.co","tipoId":1}],
                                  "telefonos":[{"telefono":"3111111111","tipoId":1}],
                                  "observacionesTelefono":"Actualizada",
                                  "observacionesCorreo":"Actualizada",
                                  "medioSolicitudId":1,
                                  "descripcionHechos":"Descripción actualizada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.primerNombre").value("Ana María"))
                .andExpect(jsonPath("$.primerApellido").value("Gómez López"))
                .andExpect(jsonPath("$.observacionesTelefono").value("Actualizada"));
    }

    @Test
    void returnsNotFoundWhenUpdatingUnknownSolicitud() throws Exception {
        mockMvc.perform(put("/solicitudes/acompanamiento/999999")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "primerNombre":"Ana",
                                  "correos":[{"correo":"ana@example.edu.co","tipoId":1}],
                                  "telefonos":[{"telefono":"3111111111","tipoId":1}],
                                  "descripcionHechos":"Hechos"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    @Test
    void rejectsUpdateWithoutDescriptionEmailsOrPhones() throws Exception {
        mockMvc.perform(put("/solicitudes/acompanamiento/100")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"descripcionHechos\":\"   \",\"correos\":[],\"telefonos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.descripcionHechos").exists())
                .andExpect(jsonPath("$.validationErrors.correos").exists())
                .andExpect(jsonPath("$.validationErrors.telefonos").exists());
    }

    @Test
    void rejectsIncompleteReparto() throws Exception {
        mockMvc.perform(post("/solicitudes/acompanamiento/100/asignar")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.grupoProfesionalId").exists())
                .andExpect(jsonPath("$.validationErrors.idTipoAsignacion").exists())
                .andExpect(jsonPath("$.validationErrors.idTipoServicio").exists())
                .andExpect(jsonPath("$.validationErrors.observaciones").exists())
                .andExpect(jsonPath("$.validationErrors.fechaReparto").exists());
    }

    @Test
    void rejectsCreateWithoutDescriptionEmailsOrPhones() throws Exception {
        mockMvc.perform(post("/solicitudes/acompanamiento")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoSolicitudId":1,
                                  "medioSolicitudId":1,
                                  "datosSolicitante":{
                                    "primerNombre":"Ana","primerApellido":"Pérez",
                                    "tipoDocumentoId":1,"numeroDocumento":"12345678",
                                    "identidadGeneroId":1,"correos":[],"telefonos":[]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.descripcionHechos").exists())
                .andExpect(jsonPath("$.validationErrors['datosSolicitante.correos']").exists())
                .andExpect(jsonPath("$.validationErrors['datosSolicitante.telefonos']").exists());
    }
}

package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void persistsNewFieldsAndAggressorWithoutCaso() throws Exception {
        String body = """
                {
                  "primerNombre":"Ana",
                  "correos":[{"correo":"ana@example.edu.co","tipoId":1}],
                  "telefonos":[{"telefono":"3111111111","tipoId":1}],
                  "vinculoUniversidadId":1,
                  "descripcionHechos":"Hechos ocurridos",
                  "lugarOcurrencia":"Bloque 5",
                  "agresorNombre":"Pedro Pérez",
                  "agresorVinculoUdeaId":1,
                  "agresorOtroVinculoUdea":"No aplica",
                  "agresorCorreo":"pedro@example.com"
                }
                """;
        mockMvc.perform(put("/solicitudes/acompanamiento/100")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("SOL-100"))
                .andExpect(jsonPath("$.descripcionHechos").value("Hechos ocurridos"))
                .andExpect(jsonPath("$.lugarOcurrencia").value("Bloque 5"))
                .andExpect(jsonPath("$.vinculoUniversidadId").value(1))
                .andExpect(jsonPath("$.agresorNombre").value("Pedro Pérez"))
                .andExpect(jsonPath("$.agresorVinculoUdeaId").value(1))
                .andExpect(jsonPath("$.agresorOtroVinculoUdea").doesNotExist());

        mockMvc.perform(get("/solicitudes/acompanamiento/100")
                        .header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agresorCorreo").value("pedro@example.com"))
                .andExpect(jsonPath("$.lugarOcurrencia").value("Bloque 5"));
    }

    private String reparto(int grupo, int tipoAsignacion, String obs) {
        return "{\"grupoProfesionalId\":" + grupo + ",\"idTipoAsignacion\":" + tipoAsignacion
                + ",\"idTipoServicio\":1,\"fechaReparto\":\"2026-11-03\",\"observaciones\":\"" + obs + "\"}";
    }

    @Test
    void repartoPersistsFieldsAndLatestOneIsCurrent() throws Exception {
        mockMvc.perform(post("/solicitudes/acompanamiento/100/asignar")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content(reparto(1, 1, "Primero")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEstadoSolicitud").value(2))
                .andExpect(jsonPath("$.grupoProfesionalId").value(1))
                .andExpect(jsonPath("$.tipoAsignacionId").value(1))
                .andExpect(jsonPath("$.tipoServicioId").value(1))
                .andExpect(jsonPath("$.fechaReparto").value("2026-11-03"))
                .andExpect(jsonPath("$.observacionesReparto").value("Primero"));

        mockMvc.perform(post("/solicitudes/acompanamiento/100/asignar")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content(reparto(2, 2, "Segundo")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/solicitudes/acompanamiento/100")
                        .header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(jsonPath("$.grupoProfesionalId").value(2))
                .andExpect(jsonPath("$.observacionesReparto").value("Segundo"));
    }

    @Test
    void repartoRejectsInactiveGroupAndUnknownCatalogs() throws Exception {
        mockMvc.perform(post("/solicitudes/acompanamiento/100/asignar")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content(reparto(3, 1, "x")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/solicitudes/acompanamiento/100/asignar")
                        .header("Authorization", bearer("admin@udea.edu.co"))
                        .contentType(APPLICATION_JSON)
                        .content(reparto(1, 99, "x")))
                .andExpect(status().isNotFound());
    }

    @Test
    void groupsEndpointListsOnlyActive() throws Exception {
        mockMvc.perform(get("/solicitudes/grupos-profesionales")
                        .header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[0].idTipoServicio").value(2))
                .andExpect(jsonPath("$[1].idTipoServicio").value(2))
                .andExpect(jsonPath("$[2].idTipoServicio").value(5));
    }
}

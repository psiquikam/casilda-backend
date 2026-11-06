package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.repository.ReporteCasoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReporteCasoIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReporteCasoRepository reporteCasoRepository;

    @Test
    void creaReporteAnonimoYLoPersiste() throws Exception {
        String response = mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoDocumentoId": 1,
                                  "numeroDocumento": "1020304050",
                                  "fechaNacimiento": "1999-03-14",
                                  "tipoLugar": "interno",
                                  "campusId": 1,
                                  "facultad": "Facultad de Medicina",
                                  "bloque": "Bloque 12",
                                  "espacio": "Aula 201",
                                  "descripcionHechos": "Texto de prueba para el reporte.",
                                  "rutaActivacionId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.codigo", matchesPattern("CAS-[A-Z2-9]{10}")))
                .andExpect(jsonPath("$.rutaActivacion.estado").value("SOLICITADA"))
                .andReturn().getResponse().getContentAsString();

        String codigo = objectMapper.readTree(response).get("codigo").asText();
        ReporteCaso reporte = reporteCasoRepository.findByCodigo(codigo).orElseThrow();

        assertEquals("Texto de prueba para el reporte.", reporte.getDescripcionHechos());
        assertEquals("SOLICITADA", reporte.getEstadoRuta().name());
        assertNull(reporte.getUsuario());
    }

    @Test
    void tokenInvalidoNoSeConvierteEnReporteAnonimo() throws Exception {
        mockMvc.perform(post("/reportes")
                        .header("Authorization", "Bearer token-invalido")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcionHechos": "No debe persistirse",
                                  "rutaActivacionId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lugarInternoSinCampusRetornaValidacion() throws Exception {
        mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoLugar": "interno",
                                  "descripcionHechos": "Falta el campus",
                                  "rutaActivacionId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.campusId")
                        .value("Seleccione el campus donde ocurrieron los hechos"));
    }

    @Test
    void lugarExternoRequiereDetalleYNoAceptaCampus() throws Exception {
        mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoLugar": "externo",
                                  "campusId": 1,
                                  "descripcionHechos": "Lugar externo inválido.",
                                  "rutaActivacionId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.lugarDetalleExterno")
                        .value("Describa el lugar externo donde ocurrieron los hechos"));
    }

    @Test
    void tarjetaDeIdentidadNoPermitePersonaMayorDeEdad() throws Exception {
        mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoDocumentoId": 2,
                                  "numeroDocumento": "1234567890",
                                  "fechaNacimiento": "1990-01-01",
                                  "descripcionHechos": "Documento incompatible con la edad.",
                                  "rutaActivacionId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.tipoDocumentoId")
                        .value("La tarjeta de identidad solo aplica a menores de 18 años. Seleccione otro tipo de documento."));
    }

    @Test
    void bearerInvalidoRetornaUnauthorized() throws Exception {
        mockMvc.perform(post("/reportes")
                        .header("Authorization", "Bearer token-invalido")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcionHechos": "No debe persistirse",
                                  "rutaActivacionId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void esquemaDeAutorizacionDistintoDeBearerSeRechaza() throws Exception {
        mockMvc.perform(post("/reportes")
                        .header("Authorization", "Basic abc")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcionHechos": "No debe persistirse",
                                  "rutaActivacionId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
}

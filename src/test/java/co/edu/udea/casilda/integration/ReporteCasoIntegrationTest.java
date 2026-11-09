package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.repository.EvidenciaReporteRepository;
import co.edu.udea.casilda.repository.ReporteCasoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReporteCasoIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReporteCasoRepository reporteCasoRepository;

    @Autowired
    private EvidenciaReporteRepository evidenciaReporteRepository;

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
    void catalogosDelFormularioAnonimoSonPublicos() throws Exception {
        mockMvc.perform(get("/maestros/tipos-identificacion"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/maestros/campus"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/maestros/rutas-activacion"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/maestros/tipos-solicitud"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/maestros/unidades-administrativas"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/maestros/unidades-academicas"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/maestros/identidades-genero"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/maestros/cargos"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/maestros/medio-solicitud"))
                .andExpect(status().isOk());
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

    @Test
    void persisteDatosAdicionalesDelReporte() throws Exception {
        String response = mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcionHechos": "Reporte con datos adicionales.",
                                  "rutaActivacionId": 1,
                                  "tipoReportante": "TERCERO",
                                  "victima": {
                                    "primerNombre": "Ana",
                                    "primerApellido": "Pérez",
                                    "correo": "ana@correo.com",
                                    "identidadGeneroId": 1,
                                    "cargoId": 1
                                  },
                                  "contacto": {
                                    "deseaContacto": true,
                                    "correo": "contacto@correo.com",
                                    "whatsapp": "3001234567"
                                  },
                                  "presuntoAgresor": {
                                    "numeroDocumento": "98765432",
                                    "primerNombre": "Carlos",
                                    "primerApellido": "Gómez",
                                    "vinculoUniversidadId": 1,
                                    "vinculoVictimaId": 1,
                                    "otroVinculoVictima": "Jefe de práctica",
                                    "informacionAdicional": "Trabaja en el bloque 12",
                                    "remitirUad": true
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String codigo = objectMapper.readTree(response).get("codigo").asText();
        ReporteCaso reporte = reporteCasoRepository.findByCodigo(codigo).orElseThrow();

        assertEquals("TERCERO", reporte.getTipoReportante().name());
        assertEquals("Ana", reporte.getVictimaPrimerNombre());
        assertEquals("contacto@correo.com", reporte.getContactoCorreo());
        assertEquals("3001234567", reporte.getContactoWhatsapp());
        assertEquals("98765432", reporte.getAgresorNumeroDocumento());
        assertEquals("Jefe de práctica", reporte.getAgresorOtroVinculoVictima());
        assertEquals(Boolean.TRUE, reporte.getAgresorRemitirUad());
        assertEquals("v1", reporte.getAgresorRemitirUadVersion());
    }

    @Test
    void contactoSolicitadoRequiereCorreoOWhatsapp() throws Exception {
        mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcionHechos": "Falta medio de contacto.",
                                  "rutaActivacionId": 1,
                                  "contacto": {
                                    "deseaContacto": true
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.contacto")
                        .value("Ingrese un correo o WhatsApp para ser contactada"));
    }

    @Test
    void tipoReportanteInvalidoRetornaValidacion() throws Exception {
        mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcionHechos": "Tipo inválido.",
                                  "rutaActivacionId": 1,
                                  "tipoReportante": "OTRO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.tipoReportante")
                        .value("El tipo de reportante debe ser AFECTADA o TERCERO"));
    }

    @Test
    void victimaRequiereNombreApellidoYCargo() throws Exception {
        mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcionHechos": "Datos incompletos de víctima.",
                                  "rutaActivacionId": 1,
                                  "victima": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors['victima.primerNombre']")
                        .value("El primer nombre de la víctima es obligatorio"));
    }

    @Test
    void referenciaDeCatalogoInexistenteRetornaValidacion() throws Exception {
        mockMvc.perform(post("/reportes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcionHechos": "Catálogo inexistente.",
                                  "rutaActivacionId": 1,
                                  "victima": {
                                    "primerNombre": "Ana",
                                    "primerApellido": "Pérez",
                                    "cargoId": 999999
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors['victima.cargoId']")
                        .value("El cargo de la víctima no existe"));
    }

    @Test
    void creaReporteMultipartConEvidenciaPdf() throws Exception {
        MockMultipartFile reporte = new MockMultipartFile(
                "reporte", "reporte.json", APPLICATION_JSON_VALUE, """
                {
                  "descripcionHechos": "Reporte multipart con evidencia.",
                  "rutaActivacionId": 1
                }
                """.getBytes());
        MockMultipartFile evidencia = new MockMultipartFile(
                "evidencias", "evidencia.pdf", "application/pdf",
                "%PDF-1.7\ncontenido de prueba".getBytes());

        String response = mockMvc.perform(multipart("/reportes")
                        .file(reporte)
                        .file(evidencia))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", matchesPattern("CAS-[A-Z2-9]{10}")))
                .andReturn().getResponse().getContentAsString();

        String codigo = objectMapper.readTree(response).get("codigo").asText();
        assertEquals(1, evidenciaReporteRepository.countByReporteCasoCodigo(codigo));
    }

    @Test
    void rechazaEvidenciaConFirmaInvalida() throws Exception {
        MockMultipartFile reporte = new MockMultipartFile(
                "reporte", "reporte.json", APPLICATION_JSON_VALUE, """
                {
                  "descripcionHechos": "Reporte multipart inválido.",
                  "rutaActivacionId": 1
                }
                """.getBytes());
        MockMultipartFile evidencia = new MockMultipartFile(
                "evidencias", "evidencia.pdf", "application/pdf",
                "no es un pdf".getBytes());

        mockMvc.perform(multipart("/reportes")
                        .file(reporte)
                        .file(evidencia))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El contenido de la evidencia no coincide con su tipo"));
    }
}

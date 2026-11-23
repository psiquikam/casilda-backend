package co.edu.udea.casilda.controller.uad;

import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.GlobalExceptionHandler;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.exception.UadExceptionHandler;
import co.edu.udea.casilda.service.uad.UadMisReportesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rutas, multipart y formato de error de «Mis reportes». */
class UadMisReportesControllerTest {

    private static final String CODIGO = "CAS-PRUEBA0001";
    private static final UUID SOLICITUD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String RUTA_RESPUESTA = "/reportes/" + CODIGO + "/solicitudes-informacion/" + SOLICITUD + "/respuesta";

    private UadMisReportesService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(UadMisReportesService.class);
        final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
        mvc = MockMvcBuilders.standaloneSetup(new UadMisReportesController(service))
                .setControllerAdvice(new UadExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .build();
    }

    private static UadResponses.MiReporteDetalle detalle() {
        return new UadResponses.MiReporteDetalle(CODIGO, "NDI-2026-000201", "UAD", "En revisión", Instant.parse("2026-11-10T15:00:00Z"), null, false, null);
    }

    @Test
    void listaSinCache() throws Exception {
        when(service.listar(any())).thenReturn(new PageImpl<>(List.of(
                new UadResponses.MiReporteResumen(CODIGO, "NDI-2026-000201", "UAD", "Recibido", Instant.parse("2026-11-10T15:00:00Z"), null, false)), PageRequest.of(0, 10), 1));
        mvc.perform(get("/mis-reportes"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.content[0].codigo").value(CODIGO))
                .andExpect(jsonPath("$.content[0].actor").value("UAD"));
    }

    @Test
    void reporteAjenoResponde404ConCodigo() throws Exception {
        when(service.obtener("CAS-DEOTRAPERS")).thenThrow(UadException.noEncontrado());
        mvc.perform(get("/mis-reportes/CAS-DEOTRAPERS"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void laRespuestaLlegaComoMultipartSoloConTexto() throws Exception {
        when(service.responder(CODIGO, SOLICITUD, "Ocurrió el 3 de noviembre.", false)).thenReturn(detalle());
        mvc.perform(multipart(RUTA_RESPUESTA).param("respuesta", "Ocurrió el 3 de noviembre."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("En revisión"));
        verify(service).responder(CODIGO, SOLICITUD, "Ocurrió el 3 de noviembre.", false);
    }

    @Test
    void losArchivosAdjuntosSeInformanAlServicioParaRechazarlos() throws Exception {
        when(service.responder(CODIGO, SOLICITUD, "Texto", true)).thenThrow(
                new UadException(org.springframework.http.HttpStatus.BAD_REQUEST, "EVIDENCIAS_NO_HABILITADAS", "Por ahora la respuesta solo admite texto."));
        mvc.perform(multipart(RUTA_RESPUESTA)
                        .file(new MockMultipartFile("evidencias", "prueba.pdf", "application/pdf", new byte[]{1, 2, 3}))
                        .param("respuesta", "Texto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("EVIDENCIAS_NO_HABILITADAS"));
    }
}

package co.edu.udea.casilda.controller.uad;

import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.GlobalExceptionHandler;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.exception.UadExceptionHandler;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.service.uad.UadNoticiaService;
import co.edu.udea.casilda.service.uad.UadRadicacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UadControllersTest {

    private UadNoticiaService noticias;
    private UadRadicacionService radicacion;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        noticias = mock(UadNoticiaService.class);
        radicacion = mock(UadRadicacionService.class);
        final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
        mvc = MockMvcBuilders.standaloneSetup(new UadNoticiaController(noticias), new UadRadicacionController(radicacion))
                .setControllerAdvice(new UadExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .build();
    }

    @Test
    void contextoSaleSinCache() throws Exception {
        when(noticias.contexto()).thenReturn(new UadResponses.ContextoUsuario(List.of(EquipoUad.EQUIPO_3), false, List.of()));
        mvc.perform(get("/uad/contexto"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.equipos[0]").value("EQUIPO_3"));
    }

    @Test
    void idMalFormadoResponde404SinLlegarAlServicio() throws Exception {
        mvc.perform(get("/uad/noticias/no-es-uuid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        verify(noticias, never()).obtener(any());
    }

    @Test
    void errorUadIncluyeCodigoYCamposConElFormatoDelBackend() throws Exception {
        final UUID id = UUID.randomUUID();
        when(noticias.decidir(eq(id), any())).thenThrow(new UadException(HttpStatus.BAD_REQUEST, "VALIDACION", "Revise los datos marcados.",
                Map.of("solicitudInformacion.fechaLimite", "La fecha límite debe ser posterior a hoy.")));
        mvc.perform(post("/uad/noticias/" + id + "/decisiones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SOLICITAR_INFORMACION\",\"version\":1,\"solicitudInformacion\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors['solicitudInformacion.fechaLimite']").exists());
    }

    @Test
    void conflictoDeVersionResponde409() throws Exception {
        final UUID id = UUID.randomUUID();
        when(noticias.decidir(eq(id), any())).thenThrow(UadException.conflictoVersion());
        mvc.perform(post("/uad/noticias/" + id + "/decisiones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"INHIBITORIO\",\"version\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
    }

    @Test
    void decisionSinVersionFallaLaValidacionDelRequest() throws Exception {
        mvc.perform(post("/uad/noticias/" + UUID.randomUUID() + "/decisiones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"INHIBITORIO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.version").exists());
    }

    @Test
    void radicarNdaResponde201SoloConConsecutivo() throws Exception {
        when(radicacion.radicarNda(any())).thenReturn(new UadResponses.RadicacionNda("NDA-2026-000001", Instant.parse("2026-11-15T20:00:00Z")));
        mvc.perform(post("/uad/noticias-anonimas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcionHechos\":\"Relato ficticio con detalle suficiente.\",\"correo\":\"no-debe-viajar@example.org\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consecutivo").value("NDA-2026-000001"))
                .andExpect(jsonPath("$.correo").doesNotExist());
    }

    @Test
    void radicarNdaCorreConSesionAnonimaYRestauraLaOriginal() throws Exception {
        final var sesion = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "persona@example.org", null, org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USUARIO"));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(sesion);
        final java.util.concurrent.atomic.AtomicReference<Object> vista = new java.util.concurrent.atomic.AtomicReference<>();
        when(radicacion.radicarNda(any())).thenAnswer(inv -> {
            vista.set(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
            return new UadResponses.RadicacionNda("NDA-2026-000002", Instant.parse("2026-11-15T20:00:00Z"));
        });
        try {
            mvc.perform(post("/uad/noticias-anonimas").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"descripcionHechos\":\"Relato ficticio con detalle suficiente.\"}"))
                    .andExpect(status().isCreated());
            org.junit.jupiter.api.Assertions.assertInstanceOf(
                    org.springframework.security.authentication.AnonymousAuthenticationToken.class, vista.get());
            org.junit.jupiter.api.Assertions.assertSame(sesion,
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void radicarNdaConRelatoCortoResponde400() throws Exception {
        mvc.perform(post("/uad/noticias-anonimas").contentType(MediaType.APPLICATION_JSON).content("{\"descripcionHechos\":\"corto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.descripcionHechos").exists());
        verify(radicacion, never()).radicarNda(any());
    }
}

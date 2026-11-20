package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import co.edu.udea.casilda.model.entity.uad.UadSolicitudInformacion;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoEventoUad;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.repository.UsuarioRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import co.edu.udea.casilda.repository.uad.UadSolicitudInformacionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** «Mis reportes»: filtro por dueño y respuesta a la solicitud de información, con repositorios simulados. */
class UadMisReportesServiceTest {

    private static final String CODIGO = "CAS-PRUEBA0001";
    private static final long DUENO = 7L;

    private UadNoticiaRepository noticias;
    private UadSolicitudInformacionRepository solicitudes;
    private UsuarioRepository usuarios;
    private UadNoticiaService noticiaService;
    private UadMisReportesService service;

    @BeforeEach
    void setUp() {
        noticias = mock(UadNoticiaRepository.class);
        solicitudes = mock(UadSolicitudInformacionRepository.class);
        usuarios = mock(UsuarioRepository.class);
        noticiaService = mock(UadNoticiaService.class);
        service = new UadMisReportesService(noticias, solicitudes, usuarios, noticiaService, new UadNoticiaMapper());
        when(solicitudes.findFirstByNoticiaIdOrderByFechaSolicitudDesc(any())).thenReturn(Optional.empty());
        sesion("persona@udea.edu.co", DUENO);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void sesion(final String email, final long id) {
        final Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        when(usuarios.findByEmailIgnoreCase(email)).thenReturn(Optional.of(u));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "x", List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
    }

    private UadNoticia noticia(final EstadoNoticia estado) {
        final ReporteCaso r = new ReporteCaso();
        r.setId(10L);
        r.setCodigo(CODIGO);
        final UadNoticia n = new UadNoticia();
        n.setId(UUID.randomUUID());
        n.setConsecutivo("NDI-2026-000201");
        n.setTipo(TipoNoticia.NDI);
        n.setFechaRecepcion(Instant.parse("2026-11-10T15:00:00Z"));
        n.setEstado(estado);
        n.setVersion(1L);
        n.setReporteCaso(r);
        n.setDescripcionHechos("Relato ficticio de prueba.");
        when(noticias.findByReporteCasoCodigoAndReporteCasoUsuarioId(CODIGO, DUENO)).thenReturn(Optional.of(n));
        return n;
    }

    private UadSolicitudInformacion solicitud(final UadNoticia n, final LocalDate limite) {
        final UadSolicitudInformacion s = new UadSolicitudInformacion();
        s.setId(UUID.randomUUID());
        s.setNoticia(n);
        s.setTipoSolicitudCodigo("ACLARACION");
        s.setInformacionRequerida("Precise la fecha de los hechos.");
        s.setFechaSolicitud(Instant.parse("2026-11-12T15:00:00Z"));
        s.setFechaLimite(limite);
        when(solicitudes.findById(s.getId())).thenReturn(Optional.of(s));
        when(solicitudes.findFirstByNoticiaIdOrderByFechaSolicitudDesc(n.getId())).thenReturn(Optional.of(s));
        return s;
    }

    private UadException error(final Runnable accion) {
        return assertThrows(UadException.class, accion::run);
    }

    @Test
    void listaSoloLosReportesDelUsuarioYNoExponeDatosInternos() {
        final UadNoticia n = noticia(EstadoNoticia.INHIBITORIA);
        when(noticias.findByReporteCasoUsuarioId(eq(DUENO), any())).thenReturn(new PageImpl<>(List.of(n)));

        final UadResponses.MiReporteResumen r = service.listar(PageRequest.of(0, 10)).getContent().get(0);

        verify(noticias).findByReporteCasoUsuarioId(eq(DUENO), any());
        assertEquals(CODIGO, r.codigo());
        assertEquals("NDI-2026-000201", r.consecutivo());
        assertEquals("UAD", r.actor());
        // La decisión interna (inhibitoria) no se revela al reportante.
        assertEquals("Revisión finalizada", r.estado());
        assertFalse(r.solicitudPendiente());
        assertNull(r.fechaUltimaActuacion());
    }

    @Test
    void reporteAjenoOInexistenteResponde404() {
        when(noticias.findByReporteCasoCodigoAndReporteCasoUsuarioId(anyString(), anyLong())).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, error(() -> service.obtener("CAS-DEOTRAPERS")).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, error(() -> service.responder("CAS-DEOTRAPERS", UUID.randomUUID(), "Texto", false)).getStatus());
    }

    @Test
    void sinSesionResponde401() {
        SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.UNAUTHORIZED, error(() -> service.listar(PageRequest.of(0, 10))).getStatus());
    }

    @Test
    void elDetalleMuestraLaSolicitudPendiente() {
        final UadNoticia n = noticia(EstadoNoticia.PENDIENTE_SUBSANACION);
        solicitud(n, LocalDate.now().plusDays(5));

        final UadResponses.MiReporteDetalle d = service.obtener(CODIGO);

        assertTrue(d.solicitudPendiente());
        assertEquals("Información adicional solicitada", d.estado());
        assertEquals("Precise la fecha de los hechos.", d.solicitudInformacion().informacionRequerida());
        assertFalse(d.solicitudInformacion().vencida());
    }

    @Test
    void responderGuardaElTextoYPasaLaNoticiaASubsanacionRecibida() {
        final UadNoticia n = noticia(EstadoNoticia.PENDIENTE_SUBSANACION);
        // Vencida a propósito: el vencimiento no impide responder ni cierra la noticia (UAD-EVA-05).
        final UadSolicitudInformacion s = solicitud(n, LocalDate.now().minusDays(3));

        final UadResponses.MiReporteDetalle d = service.responder(CODIGO, s.getId(), "  Ocurrió el 3 de noviembre.  ", false);

        assertTrue(s.isRespondida());
        assertEquals("Ocurrió el 3 de noviembre.", s.getRespuesta());
        assertNotNull(s.getFechaRespuesta());
        assertEquals(EstadoNoticia.SUBSANACION_RECIBIDA, n.getEstado());
        verify(noticias).saveAndFlush(n);
        verify(noticiaService).evento(eq(n), isNull(), eq(TipoEventoUad.SUBSANACION_RECIBIDA), anyString(), isNull(),
                eq("PENDIENTE_SUBSANACION"), eq("SUBSANACION_RECIBIDA"), isNull());
        assertFalse(d.solicitudPendiente());
        assertEquals("En revisión", d.estado());
        assertEquals("Ocurrió el 3 de noviembre.", d.solicitudInformacion().respuesta());
    }

    @Test
    void respuestaVaciaLargaOConArchivosEs400YNoCambiaNada() {
        final UadNoticia n = noticia(EstadoNoticia.PENDIENTE_SUBSANACION);
        final UadSolicitudInformacion s = solicitud(n, LocalDate.now().plusDays(5));

        assertEquals("VALIDACION", error(() -> service.responder(CODIGO, s.getId(), "   ", false)).getCodigo());
        assertEquals("VALIDACION", error(() -> service.responder(CODIGO, s.getId(), "x".repeat(UadMisReportesService.LIMITE_RESPUESTA + 1), false)).getCodigo());
        final UadException archivos = error(() -> service.responder(CODIGO, s.getId(), "Texto", true));
        assertEquals(HttpStatus.BAD_REQUEST, archivos.getStatus());
        assertEquals("EVIDENCIAS_NO_HABILITADAS", archivos.getCodigo());

        assertFalse(s.isRespondida());
        assertEquals(EstadoNoticia.PENDIENTE_SUBSANACION, n.getEstado());
        verify(noticias, never()).saveAndFlush(any());
    }

    @Test
    void noSeRespondeDosVecesNiCuandoLaUadYaDecidio() {
        final UadNoticia n = noticia(EstadoNoticia.PENDIENTE_SUBSANACION);
        final UadSolicitudInformacion s = solicitud(n, LocalDate.now().plusDays(5));
        s.setRespondida(true);
        assertEquals("SOLICITUD_YA_RESPONDIDA", error(() -> service.responder(CODIGO, s.getId(), "Texto", false)).getCodigo());

        s.setRespondida(false);
        n.setEstado(EstadoNoticia.REMITIDA_POR_COMPETENCIA);
        final UadException e = error(() -> service.responder(CODIGO, s.getId(), "Texto", false));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("SOLICITUD_NO_VIGENTE", e.getCodigo());
    }

    @Test
    void solicitudDeOtraNoticiaResponde404() {
        noticia(EstadoNoticia.PENDIENTE_SUBSANACION);
        final UadNoticia otra = new UadNoticia();
        otra.setId(UUID.randomUUID());
        final UadSolicitudInformacion ajena = new UadSolicitudInformacion();
        ajena.setId(UUID.randomUUID());
        ajena.setNoticia(otra);
        when(solicitudes.findById(ajena.getId())).thenReturn(Optional.of(ajena));

        assertEquals(HttpStatus.NOT_FOUND, error(() -> service.responder(CODIGO, ajena.getId(), "Texto", false)).getStatus());
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}

package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.request.uad.ClasificacionUadRequest;
import co.edu.udea.casilda.dto.request.uad.DecisionEvaluacionRequest;
import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.model.entity.uad.UadCaso;
import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import co.edu.udea.casilda.model.enums.TipoReportanteEnum;
import co.edu.udea.casilda.model.enums.uad.AccionNoticia;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import co.edu.udea.casilda.model.enums.uad.ModoCaso;
import co.edu.udea.casilda.model.enums.uad.TipoDecision;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoOrigenNoticia;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.EvidenciaReporteRepository;
import co.edu.udea.casilda.repository.uad.UadCasoRepository;
import co.edu.udea.casilda.repository.uad.UadEventoRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import co.edu.udea.casilda.repository.uad.UadSolicitudInformacionRepository;
import co.edu.udea.casilda.service.ReporteEvidenciaStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de autorización y de dominio de {@link UadNoticiaService}, con repositorios simulados. */
class UadNoticiaServiceTest {

    private UadNoticiaRepository noticias;
    private UadCasoRepository casosUad;
    private CasoRepository casosAtencion;
    private UadUsuarioActual usuario;
    private UadCatalogoService catalogos;
    private UadNoticiaService service;

    private static final UadUsuarioActual.Actor EQUIPO_3 = new UadUsuarioActual.Actor("e3@udea.edu.co", "Profesional E3", List.of(EquipoUad.EQUIPO_3), false);
    private static final UadUsuarioActual.Actor EQUIPO_4 = new UadUsuarioActual.Actor("e4@udea.edu.co", "Profesional E4", List.of(EquipoUad.EQUIPO_4), false);

    @BeforeEach
    void setUp() {
        noticias = mock(UadNoticiaRepository.class);
        casosUad = mock(UadCasoRepository.class);
        casosAtencion = mock(CasoRepository.class);
        usuario = mock(UadUsuarioActual.class);
        catalogos = mock(UadCatalogoService.class);
        final UadSolicitudInformacionRepository solicitudes = mock(UadSolicitudInformacionRepository.class);
        when(solicitudes.findFirstByNoticiaIdOrderByFechaSolicitudDesc(any())).thenReturn(Optional.empty());
        when(noticias.saveAndFlush(any())).thenAnswer(i -> {
            final UadNoticia n = i.getArgument(0);
            n.setVersion(n.getVersion() + 1);
            return n;
        });
        when(casosUad.saveAndFlush(any())).thenAnswer(i -> {
            final UadCaso c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setVersion(0L);
            return c;
        });
        when(catalogos.existe(any(), anyString())).thenReturn(true);
        when(catalogos.nombreDe(any(), anyString())).thenAnswer(i -> i.getArgument(1));
        service = new UadNoticiaService(noticias, casosUad, mock(UadEventoRepository.class), solicitudes, casosAtencion,
                mock(EvidenciaReporteRepository.class), mock(ReporteEvidenciaStorageService.class), usuario, catalogos, new UadNoticiaMapper());
    }

    private UadNoticia ndi(final VinculacionInvestigado v, final TipoReportanteEnum tipoReportante) {
        final ReporteCaso r = new ReporteCaso();
        r.setId(10L);
        r.setNumeroDocumento("0000000141");
        r.setTipoReportante(tipoReportante);
        final UadNoticia n = new UadNoticia();
        n.setId(UUID.randomUUID());
        n.setConsecutivo("NDI-2026-000141");
        n.setTipo(TipoNoticia.NDI);
        n.setOrigenTipo(TipoOrigenNoticia.FORMULARIO_WEB_CASILDA);
        n.setFechaRecepcion(Instant.now());
        n.setEstado(EstadoNoticia.EN_EVALUACION_PRELIMINAR);
        n.setVersion(2L);
        n.setVinculacionDeclarada(v);
        n.setReporteCaso(r);
        n.setDescripcionHechos("Relato ficticio de prueba.");
        when(noticias.findById(n.getId())).thenReturn(Optional.of(n));
        return n;
    }

    private DecisionEvaluacionRequest continuar(final long version, final ModoCaso modo, final String vincular) {
        return new DecisionEvaluacionRequest(TipoDecision.CONTINUAR_TRAMITE, version, null,
                new DecisionEvaluacionRequest.ContinuarTramite(modo, vincular, true, null), null, null, null);
    }

    @Test
    void sinEquipoUadResponde403() {
        when(usuario.conEquipo()).thenThrow(new UadException(HttpStatus.FORBIDDEN, "SIN_EQUIPO_UAD", "x"));
        final UadException e = assertThrows(UadException.class, () -> service.obtener(UUID.randomUUID()));
        assertEquals("SIN_EQUIPO_UAD", e.getCodigo());
    }

    @Test
    void noticiaDeOtroEquipoResponde404() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_4);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_VINCULADO, TipoReportanteEnum.AFECTADA);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(UadException.class, () -> service.obtener(n.getId())).getStatus());
    }

    @Test
    void abrirLaEvaluacionNoCreaCasoIdNiCambiaEstado() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_VINCULADO, TipoReportanteEnum.AFECTADA);
        final UadResponses.NoticiaDetalle d = service.obtener(n.getId());
        assertNull(d.casoId());
        assertEquals(EstadoNoticia.EN_EVALUACION_PRELIMINAR, d.estado());
        verify(noticias, never()).saveAndFlush(any());
        assertTrue(d.accionesDisponibles().contains(AccionNoticia.CONTINUAR_TRAMITE));
    }

    @Test
    void continuarConCasoNuevoGeneraIdEnElServidorSinExpediente() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_VINCULADO, TipoReportanteEnum.AFECTADA);
        final UadResponses.DecisionEvaluacion r = service.decidir(n.getId(), continuar(2L, ModoCaso.CREAR_NUEVO, null));
        assertEquals(EstadoNoticia.PROCEDE_TRAMITE, r.noticia().estado());
        assertTrue(r.noticia().casoId().matches("CAS-[A-Z0-9]{10}"));
        assertEquals("NDI-2026-000141", r.noticia().consecutivo());
        assertNull(r.noticia().expedienteUad());
        assertEquals(EquipoUad.EQUIPO_3, r.casoUad().equipoResponsable());
        assertEquals(FaseUad.INSTRUCCION, r.casoUad().faseActual());
        assertFalse(r.notificaciones().stream().anyMatch(x -> "ENVIADA".equals(x.estado())));
    }

    @Test
    void versionViejaResponde409() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_VINCULADO, TipoReportanteEnum.AFECTADA);
        assertEquals("CONFLICTO_VERSION", assertThrows(UadException.class, () -> service.decidir(n.getId(), continuar(1L, ModoCaso.CREAR_NUEVO, null))).getCodigo());
    }

    @Test
    void noticiaConDecisionNoAdmiteOtra() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_VINCULADO, TipoReportanteEnum.AFECTADA);
        n.setEstado(EstadoNoticia.INHIBITORIA);
        assertEquals("NOTICIA_YA_DECIDIDA", assertThrows(UadException.class, () -> service.decidir(n.getId(), continuar(2L, ModoCaso.CREAR_NUEVO, null))).getCodigo());
    }

    @Test
    void vincularSoloACasoDeLaConsultaYConservaConsecutivo() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_VINCULADO, TipoReportanteEnum.AFECTADA);
        when(casosAtencion.findByCitaSolicitudAtencionSolicitanteNumeroDocumento("0000000141")).thenReturn(List.of());
        final UadException e = assertThrows(UadException.class, () -> service.decidir(n.getId(), continuar(2L, ModoCaso.VINCULAR_EXISTENTE, "CAS-AJENO")));
        assertNotNull(e.getValidationErrors().get("continuarTramite.casoIdVincular"));
    }

    @Test
    void sinVinculacionDeterminadaNoSePuedeContinuar() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_4);
        final UadNoticia n = ndi(VinculacionInvestigado.NO_SE, TipoReportanteEnum.AFECTADA);
        final UadResponses.NoticiaDetalle d = service.obtener(n.getId());
        assertFalse(d.accionesDisponibles().contains(AccionNoticia.CONTINUAR_TRAMITE));
        assertNotNull(d.motivosNoDisponible().get(AccionNoticia.CONTINUAR_TRAMITE));
    }

    @Test
    void clasificarAlOtroEquipoQuitaLaVisibilidad() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadNoticia n = ndi(VinculacionInvestigado.NO_SE, TipoReportanteEnum.AFECTADA);
        final UadResponses.Clasificacion r = service.clasificar(n.getId(), new ClasificacionUadRequest(VinculacionInvestigado.ESTUDIANTE_PREGRADO, 2L));
        assertFalse(r.visibleParaUsuario());
        assertNull(r.noticia());
    }

    @Test
    void enSolicitudIndirectaNoHayPersonaParaConsultarCasos() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_VINCULADO, TipoReportanteEnum.TERCERO);
        assertEquals("NOTICIA_NO_EVALUABLE", assertThrows(UadException.class, () -> service.casosAsociados(n.getId())).getCodigo());
    }

    @Test
    void ndaNoExponeReportanteNiContacto() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_4);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_CATEDRA, TipoReportanteEnum.AFECTADA);
        n.setTipo(TipoNoticia.NDA);
        n.setReporteCaso(null);
        final UadResponses.NoticiaDetalle d = service.obtener(n.getId());
        assertNull(d.reportante());
        assertNull(d.personaAfectada());
        assertFalse(d.accionesDisponibles().contains(AccionNoticia.SOLICITAR_INFORMACION));
    }

    @Test
    void solicitudDeInformacionNoCreaCasoId() {
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadNoticia n = ndi(VinculacionInvestigado.DOCENTE_VINCULADO, TipoReportanteEnum.AFECTADA);
        final DecisionEvaluacionRequest c = new DecisionEvaluacionRequest(TipoDecision.SOLICITAR_INFORMACION, 2L, null, null,
                new DecisionEvaluacionRequest.SolicitudInformacion("ACLARACION_HECHOS", "Precise las fechas de los hechos.", LocalDate.now().plusDays(10)),
                null, null);
        final UadResponses.DecisionEvaluacion r = service.decidir(n.getId(), c);
        assertEquals(EstadoNoticia.PENDIENTE_SUBSANACION, r.noticia().estado());
        assertNull(r.noticia().casoId());
        assertNull(r.casoUad());
    }
}

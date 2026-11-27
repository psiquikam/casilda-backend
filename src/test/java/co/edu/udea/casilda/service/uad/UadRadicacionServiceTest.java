package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.request.uad.RadicacionNdaRequest;
import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.model.entity.RutaActivacion;
import co.edu.udea.casilda.model.entity.VinculoUdeA;
import co.edu.udea.casilda.model.entity.uad.UadEvento;
import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import co.edu.udea.casilda.model.enums.uad.EstadoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.model.enums.uad.TipoOrigenNoticia;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import co.edu.udea.casilda.repository.ReporteCasoRepository;
import co.edu.udea.casilda.repository.uad.UadEventoRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UadRadicacionServiceTest {

    private UadNoticiaRepository noticias;
    private UadEventoRepository eventos;
    private ReporteCasoRepository reportes;
    private UadConsecutivoService consecutivos;
    private UadRadicacionService service;

    @BeforeEach
    void setUp() {
        noticias = mock(UadNoticiaRepository.class);
        eventos = mock(UadEventoRepository.class);
        reportes = mock(ReporteCasoRepository.class);
        consecutivos = mock(UadConsecutivoService.class);
        when(consecutivos.siguiente(TipoNoticia.NDA)).thenReturn("NDA-2026-000001");
        when(consecutivos.siguiente(TipoNoticia.NDI)).thenReturn("NDI-2026-000001");
        when(noticias.findByReporteCasoId(any())).thenReturn(Optional.empty());
        service = new UadRadicacionService(noticias, eventos, reportes, consecutivos);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ndaNoAsociaIdentidadAunqueHayaSesion() {
        // Hay una sesión válida en el contexto: la NDA no debe usarla.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("persona@udea.edu.co", null, List.of()));
        final var r = service.radicarNda(new RadicacionNdaRequest("  Relato ficticio con detalle suficiente.  ", null, " ", VinculacionInvestigado.NO_SE));

        assertEquals("NDA-2026-000001", r.consecutivo());
        final ArgumentCaptor<UadNoticia> noticia = ArgumentCaptor.forClass(UadNoticia.class);
        verify(noticias).save(noticia.capture());
        assertEquals(TipoNoticia.NDA, noticia.getValue().getTipo());
        assertEquals(TipoOrigenNoticia.FORMULARIO_ANONIMO, noticia.getValue().getOrigenTipo());
        assertEquals(EstadoNoticia.RECIBIDA, noticia.getValue().getEstado());
        assertNull(noticia.getValue().getReporteCaso());
        assertNull(noticia.getValue().getPresuntoResponsable());
        assertEquals("Relato ficticio con detalle suficiente.", noticia.getValue().getDescripcionHechos());

        final ArgumentCaptor<UadEvento> evento = ArgumentCaptor.forClass(UadEvento.class);
        verify(eventos).save(evento.capture());
        assertNull(evento.getValue().getProfesional());
        assertNull(evento.getValue().getEquipo());
    }

    private ReporteCaso reporte(final String ruta, final Boolean remitirUad, final String vinculo) {
        final RutaActivacion r = new RutaActivacion();
        r.setCodigo(ruta);
        final ReporteCaso rep = new ReporteCaso();
        rep.setId(7L);
        rep.setRutaActivacion(r);
        rep.setAgresorRemitirUad(remitirUad);
        rep.setDescripcionHechos("Relato ficticio.");
        rep.setFechaRegistro(Instant.parse("2026-11-02T15:24:00Z"));
        if (vinculo != null) {
            final VinculoUdeA v = new VinculoUdeA();
            v.setNombre(vinculo);
            rep.setAgresorVinculoUdeA(v);
        }
        return rep;
    }

    @Test
    void reporteConRutaUadCreaNdiConVinculacionHomologada() {
        service.crearNdiSiCorresponde(reporte("UAD_3_4", null, "Docente Vinculado"));
        final ArgumentCaptor<UadNoticia> noticia = ArgumentCaptor.forClass(UadNoticia.class);
        verify(noticias).save(noticia.capture());
        assertEquals(TipoNoticia.NDI, noticia.getValue().getTipo());
        assertEquals("NDI-2026-000001", noticia.getValue().getConsecutivo());
        assertEquals(VinculacionInvestigado.DOCENTE_VINCULADO, noticia.getValue().getVinculacionDeclarada());
        assertEquals(EstadoNoticia.RECIBIDA, noticia.getValue().getEstado());
        assertNull(noticia.getValue().getCasoId());
    }

    @Test
    void remitirUadConOtraRutaTambienCreaNdiYSinVinculoQuedaPorClasificar() {
        service.crearNdiSiCorresponde(reporte("RUTA_PSICO", true, null));
        final ArgumentCaptor<UadNoticia> noticia = ArgumentCaptor.forClass(UadNoticia.class);
        verify(noticias).save(noticia.capture());
        assertEquals(VinculacionInvestigado.NO_SE, noticia.getValue().getVinculacionDeclarada());
        assertEquals(EstadoNoticia.PENDIENTE_CLASIFICACION, noticia.getValue().getEstado());
    }

    @Test
    void reporteDeOtraRutaSinRemisionNoCreaNada() {
        service.crearNdiSiCorresponde(reporte("RUTA_PSICO", false, "Estudiante de Pregrado"));
        verify(noticias, never()).save(any());
    }

    @Test
    void esIdempotente() {
        when(noticias.findByReporteCasoId(7L)).thenReturn(Optional.of(new UadNoticia()));
        service.crearNdiSiCorresponde(reporte("UAD_3_4", null, "Estudiante de Pregrado"));
        verify(noticias, never()).save(any());
    }

    @Test
    void unFalloAlCrearLaNdiNoPropagaAlReporte() {
        when(reportes.findById(7L)).thenThrow(new IllegalStateException("fallo simulado"));
        service.alRegistrarReporte(new UadRadicacionService.ReporteCasoRegistradoEvent(7L));
        verify(noticias, never()).save(any());
    }
}

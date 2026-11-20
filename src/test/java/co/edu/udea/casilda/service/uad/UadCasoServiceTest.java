package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.request.uad.ExpedienteRequest;
import co.edu.udea.casilda.dto.request.uad.RegistrarActuacionesRequest;
import co.edu.udea.casilda.dto.request.uad.RemisionCompetenciaCasoRequest;
import co.edu.udea.casilda.dto.request.uad.TrasladoRequest;
import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.model.entity.uad.UadCaso;
import co.edu.udea.casilda.model.entity.uad.UadEvento;
import co.edu.udea.casilda.model.enums.uad.CodigoActuacion;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoGestionUad;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import co.edu.udea.casilda.model.enums.uad.TipoEventoUad;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import co.edu.udea.casilda.repository.uad.UadCasoRepository;
import co.edu.udea.casilda.repository.uad.UadEventoRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de visibilidad, fase y concurrencia de {@link UadCasoService}, con repositorios simulados. */
class UadCasoServiceTest {

    private static final UadUsuarioActual.Actor EQUIPO_3 = new UadUsuarioActual.Actor("e3@udea.edu.co", "Profesional E3", List.of(EquipoUad.EQUIPO_3), false);
    private static final UadUsuarioActual.Actor EQUIPO_4 = new UadUsuarioActual.Actor("e4@udea.edu.co", "Profesional E4", List.of(EquipoUad.EQUIPO_4), false);

    private UadCasoRepository casos;
    private UadEventoRepository eventos;
    private UadUsuarioActual usuario;
    private UadCatalogoService catalogos;
    private EntityManager entityManager;
    private UadCasoService service;

    @BeforeEach
    void setUp() {
        casos = mock(UadCasoRepository.class);
        eventos = mock(UadEventoRepository.class);
        usuario = mock(UadUsuarioActual.class);
        catalogos = mock(UadCatalogoService.class);
        entityManager = mock(EntityManager.class);
        final UadNoticiaRepository noticias = mock(UadNoticiaRepository.class);
        when(noticias.findByCasoUadIdInOrderByFechaRecepcionAsc(any())).thenReturn(List.of());
        when(casos.saveAndFlush(any())).thenAnswer(i -> {
            final UadCaso c = i.getArgument(0);
            c.setVersion(c.getVersion() + 1);
            return c;
        });
        final AtomicLong ids = new AtomicLong();
        when(eventos.save(any())).thenAnswer(i -> {
            final UadEvento e = i.getArgument(0);
            e.setId(ids.incrementAndGet());
            return e;
        });
        when(catalogos.existe(any(), anyString())).thenReturn(true);
        when(catalogos.nombreDe(any(), anyString())).thenAnswer(i -> i.getArgument(1));
        service = new UadCasoService(casos, noticias, eventos, usuario, catalogos, new UadNoticiaMapper(), entityManager);
    }

    /** Caso de un docente (instruye Equipo 3, decide Equipo 4) en instrucción. */
    private UadCaso casoDocente() {
        final UadCaso c = new UadCaso();
        c.setId(UUID.randomUUID());
        c.setCasoId("CAS-PRUEBA0001");
        c.setVinculacionInvestigado(VinculacionInvestigado.DOCENTE_CATEDRA);
        c.setFechaAdmision(Instant.parse("2026-11-10T15:00:00Z"));
        c.setFaseActual(FaseUad.INSTRUCCION);
        c.setEquipoResponsable(EquipoUad.EQUIPO_3);
        c.setEstadoGestion(EstadoGestionUad.ACTIVA);
        c.setVersion(3L);
        when(casos.findById(c.getId())).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void otroEquipoRecibe404() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_4);
        final UadException ex = assertThrows(UadException.class, () -> service.obtener(c.getId()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void detalleIncluyeEquiposInstructorYDecisor() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadResponses.CasoUadDetalle d = service.obtener(c.getId());
        assertEquals(EquipoUad.EQUIPO_3, d.equipoInstructor());
        assertEquals(EquipoUad.EQUIPO_4, d.equipoDecisor());
    }

    @Test
    void actuacionOrdinariaRegistraEventoYSubeVersionSinCambiarFase() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadResponses.ResultadoAccionCaso r = service.registrarActuaciones(c.getId(),
                new RegistrarActuacionesRequest(List.of(CodigoActuacion.PLIEGO_CARGOS, CodigoActuacion.PRACTICA_PRUEBAS), " ", 3L));
        assertEquals(2, r.eventos().size());
        assertEquals(TipoEventoUad.ACTUACION, r.eventos().get(0).tipo());
        assertNull(r.eventos().get(0).observaciones());
        assertEquals(FaseUad.INSTRUCCION, r.caso().faseActual());
        verify(entityManager).lock(c, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    }

    @Test
    void actuacionDeOtraFaseOPermanenteRespondeActuacionNoDisponible() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadException fallo = assertThrows(UadException.class, () -> service.registrarActuaciones(c.getId(),
                new RegistrarActuacionesRequest(List.of(CodigoActuacion.FALLO), null, 3L)));
        assertEquals("ACTUACION_NO_DISPONIBLE", fallo.getCodigo());
        final UadException permanente = assertThrows(UadException.class, () -> service.registrarActuaciones(c.getId(),
                new RegistrarActuacionesRequest(List.of(CodigoActuacion.PRACTICA_PRUEBAS, CodigoActuacion.FINALIZAR_INSTRUCCION), null, 3L)));
        assertEquals("ACTUACION_NO_DISPONIBLE", permanente.getCodigo());
        verify(eventos, never()).save(any());
    }

    @Test
    void versionDistintaResponde409() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadException ex = assertThrows(UadException.class, () -> service.registrarActuaciones(c.getId(),
                new RegistrarActuacionesRequest(List.of(CodigoActuacion.PLIEGO_CARGOS), null, 2L)));
        assertEquals("CONFLICTO_VERSION", ex.getCodigo());
    }

    @Test
    void trasladoPasaAlDecisorYElInstructorDejaDeVerlo() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadResponses.ResultadoAccionCaso r = service.finalizarInstruccion(c.getId(), new TrasladoRequest(true, null, 3L));
        assertFalse(r.visibleParaUsuario());
        assertNull(r.caso());
        assertEquals(FaseUad.DECISION, c.getFaseActual());
        assertEquals(EquipoUad.EQUIPO_4, c.getEquipoResponsable());
        assertNotNull(c.getFechaUltimoTraslado());
        // El evento queda firmado por el equipo que actuó y en la fase en que actuó.
        final UadResponses.EventoHistorial e = r.eventos().get(0);
        assertEquals(TipoEventoUad.TRASLADO_FASE, e.tipo());
        assertEquals(EquipoUad.EQUIPO_3, e.equipo());
        assertEquals(FaseUad.INSTRUCCION, e.fase());
        // Sin víctima identificada no hay a quién notificar.
        assertEquals("NO_APLICA", r.notificaciones().get(0).estado());
    }

    @Test
    void trasladoSinConfirmacionOFueraDeInstruccion() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadException sinConfirmar = assertThrows(UadException.class,
                () -> service.finalizarInstruccion(c.getId(), new TrasladoRequest(false, null, 3L)));
        assertTrue(sinConfirmar.getValidationErrors().containsKey("confirmaTraslado"));

        c.setFaseActual(FaseUad.DECISION);
        c.setEquipoResponsable(EquipoUad.EQUIPO_4);
        when(usuario.conEquipo()).thenReturn(EQUIPO_4);
        final UadException enDecision = assertThrows(UadException.class,
                () -> service.finalizarInstruccion(c.getId(), new TrasladoRequest(true, null, 3L)));
        assertEquals("FASE_NO_PERMITE", enDecision.getCodigo());
    }

    @Test
    void remisionFinalizaLaGestionYBloqueaNuevasAcciones() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadResponses.ResultadoAccionCaso r = service.remitirPorCompetencia(c.getId(),
                new RemisionCompetenciaCasoRequest("FISCALIA", LocalDate.now(UadNoticiaService.BOGOTA), null, 3L));
        assertEquals(EstadoGestionUad.FINALIZADA, r.caso().estadoGestion());
        assertEquals("REMITIDA_POR_COMPETENCIA", r.caso().motivoFinGestion());
        // El caso sigue visible (historial), pero no admite más acciones.
        final UadException ex = assertThrows(UadException.class, () -> service.registrarActuaciones(c.getId(),
                new RegistrarActuacionesRequest(List.of(CodigoActuacion.PLIEGO_CARGOS), null, c.getVersion())));
        assertEquals("CASO_NO_ACTIVO", ex.getCodigo());
    }

    @Test
    void remisionConFechaFuturaOEntidadDesconocida() {
        final UadCaso c = casoDocente();
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        when(catalogos.existe(any(), eq("NO_EXISTE"))).thenReturn(false);
        final UadException ex = assertThrows(UadException.class, () -> service.remitirPorCompetencia(c.getId(),
                new RemisionCompetenciaCasoRequest("NO_EXISTE", LocalDate.now(UadNoticiaService.BOGOTA).plusDays(1), null, 3L)));
        assertTrue(ex.getValidationErrors().containsKey("entidadDestinatariaCodigo"));
        assertTrue(ex.getValidationErrors().containsKey("fechaRemision"));
        assertEquals(EstadoGestionUad.ACTIVA, c.getEstadoGestion());
    }

    @Test
    void expedienteGuardaValorAnteriorYRechazaElMismo() {
        final UadCaso c = casoDocente();
        c.setExpedienteUad("UAD-2026-0001");
        when(usuario.conEquipo()).thenReturn(EQUIPO_3);
        final UadResponses.ResultadoAccionCaso r = service.registrarExpediente(c.getId(), new ExpedienteRequest(" UAD-2026-0002 ", 3L));
        assertEquals("UAD-2026-0002", r.caso().expedienteUad());
        assertEquals("UAD-2026-0001", r.eventos().get(0).estadoAnterior());
        assertEquals("UAD-2026-0002", r.eventos().get(0).estadoNuevo());

        final UadException mismo = assertThrows(UadException.class,
                () -> service.registrarExpediente(c.getId(), new ExpedienteRequest("UAD-2026-0002", c.getVersion())));
        assertTrue(mismo.getValidationErrors().containsKey("expedienteUad"));
    }
}

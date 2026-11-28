package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.PestanaDatosComplementariosRequest;
import co.edu.udea.casilda.dto.request.PestanaDatosPersonaRequest;
import co.edu.udea.casilda.dto.request.PestanaRegistroAtencionRequest;
import co.edu.udea.casilda.dto.response.AtencionResponse;
import co.edu.udea.casilda.dto.response.CasoResponse;
import co.edu.udea.casilda.model.entity.Atencion;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.Cita;
import co.edu.udea.casilda.model.entity.EstadoCita;
import co.edu.udea.casilda.model.entity.SolicitudAtencion;
import co.edu.udea.casilda.model.entity.TipoServicio;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.model.enums.EstadoCitaEnum;
import co.edu.udea.casilda.repository.ArchivoConsentimientoRepository;
import co.edu.udea.casilda.repository.AtencionRepository;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.CitaRepository;
import co.edu.udea.casilda.repository.TipoServicioRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrimeraAtencionConsentimientoTest {
    @Mock private CasoService casoService;
    @Mock private CitaService citaService;
    @Mock private CitaRepository citaRepository;
    @Mock private CasoRepository casoRepository;
    @Mock private AtencionRepository atencionRepository;
    @Mock private ArchivoConsentimientoRepository archivoConsentimientoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TipoServicioRepository tipoServicioRepository;
    @InjectMocks private AtencionService atencionService;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rechazaConsentimientoAusenteOVacioSinCrearCaso() {
        PestanaRegistroAtencionRequest request = nuevaPrimeraAtencion();
        assertThrows(IllegalArgumentException.class, () -> atencionService.registrarPestanaAtencion(request));
        request.setArchivoConsentimientoContenido("%%%");
        assertThrows(IllegalArgumentException.class, () -> atencionService.registrarPestanaAtencion(request));
        verify(casoService, never()).registrarPestanaDatosPersona(any());
    }

    @Test
    void creaCasoYGuardaConsentimientoEnLaPrimeraAtencion() {
        PestanaRegistroAtencionRequest request = nuevaPrimeraAtencion();
        request.setArchivoConsentimientoContenido("cGRm");
        Cita cita = citaAtendida();
        Caso caso = new Caso();
        caso.setId(10L);
        caso.setCita(cita);
        Atencion atencion = new Atencion();
        atencion.setId(20L);
        atencion.setCaso(caso);
        atencion.setCita(cita);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("profesional", "test", List.of()));
        when(usuarioRepository.findByEmail("profesional")).thenReturn(Optional.of(new Usuario()));
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(casoService.registrarPestanaDatosPersona(any())).thenReturn(
                CasoResponse.builder().id(10L).codigo("CAS-10").build());
        when(casoRepository.findById(10L)).thenReturn(Optional.of(caso));
        when(atencionRepository.findByCasoIdAndCitaId(10L, 1L)).thenReturn(Optional.of(atencion));

        AtencionResponse response = atencionService.registrarPestanaAtencion(request);
        assertEquals(10L, response.getCasoId());
        assertEquals("CAS-10", response.getCodigoCaso());
        assertEquals(20L, response.getId());
        verify(casoService).registrarPestanaDatosComplementarios(request.getDatosComplementarios());
        verify(archivoConsentimientoRepository).save(any());
        verify(citaService, never()).registrarResultado(any(), any());
    }

    @Test
    void seguimientoActualizaServicioSinSolicitarConsentimiento() {
        Cita cita = citaAtendida();
        Caso caso = new Caso();
        caso.setId(10L);
        caso.setCita(cita);
        Atencion atencion = new Atencion();
        atencion.setId(20L);
        atencion.setCaso(caso);
        atencion.setCita(cita);
        TipoServicio servicio = new TipoServicio();
        servicio.setId(3);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("profesional", "test", List.of()));
        when(usuarioRepository.findByEmail("profesional")).thenReturn(Optional.of(new Usuario()));
        when(casoRepository.findById(10L)).thenReturn(Optional.of(caso));
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(atencionRepository.findById(20L)).thenReturn(Optional.of(atencion));
        when(tipoServicioRepository.findById(3)).thenReturn(Optional.of(servicio));
        PestanaRegistroAtencionRequest request = new PestanaRegistroAtencionRequest();
        request.setCasoId(10L);
        request.setCitaId(1L);
        request.setIdAtencion(20L);
        request.setIdTipoServicio(3);

        atencionService.registrarPestanaAtencion(request);
        assertEquals(servicio, atencion.getTipoServicio());
        verify(casoService, never()).registrarPestanaDatosPersona(any());
        verify(archivoConsentimientoRepository, never()).save(any());
    }

    private PestanaRegistroAtencionRequest nuevaPrimeraAtencion() {
        PestanaRegistroAtencionRequest request = new PestanaRegistroAtencionRequest();
        request.setCitaId(1L);
        request.setDatosPersona(new PestanaDatosPersonaRequest());
        request.setDatosComplementarios(new PestanaDatosComplementariosRequest());
        return request;
    }

    private Cita citaAtendida() {
        SolicitudAtencion solicitud = new SolicitudAtencion();
        solicitud.setId(2L);
        EstadoCita estado = new EstadoCita();
        estado.setId(EstadoCitaEnum.ATENDIDA.getId());
        Cita cita = new Cita();
        cita.setId(1L);
        cita.setSolicitudAtencion(solicitud);
        cita.setEstadoCita(estado);
        return cita;
    }
}

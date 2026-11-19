package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.CancelarCitaRequest;
import co.edu.udea.casilda.dto.request.RegistrarResultadoCitaRequest;
import co.edu.udea.casilda.dto.request.ReprogramarCitaRequest;
import co.edu.udea.casilda.dto.response.CitaResponse;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.Cita;
import co.edu.udea.casilda.model.entity.EstadoCita;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.model.entity.SolicitudAtencion;
import co.edu.udea.casilda.model.enums.EstadoCitaEnum;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.CitaRepository;
import co.edu.udea.casilda.repository.EstadoCitaRepository;
import co.edu.udea.casilda.repository.MotivoEstadoCitaRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {
    @Mock private CitaRepository citaRepository;
    @Mock private CasoRepository casoRepository;
    @Mock private EstadoCitaRepository estadoCitaRepository;
    @Mock private MotivoEstadoCitaRepository motivoEstadoCitaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private CitaService service;

    @Test
    void noPermiteCambiarCitasEnEstadoFinal() {
        Cita cita = new Cita();
        cita.setFecha(LocalDateTime.now().plusDays(1));
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        RegistrarResultadoCitaRequest resultado = new RegistrarResultadoCitaRequest();
        resultado.setResultado("INASISTENCIA");

        for (EstadoCitaEnum estado : List.of(EstadoCitaEnum.ATENDIDA, EstadoCitaEnum.INASISTENCIA, EstadoCitaEnum.CANCELADA)) {
            cita.setEstadoCita(new EstadoCita(estado.getId(), estado.getNombre()));
            assertThrows(IllegalArgumentException.class, () -> service.registrarResultado(1L, resultado));
            assertThrows(IllegalArgumentException.class, () -> service.reprogramar(1L, new ReprogramarCitaRequest()));
            assertThrows(IllegalArgumentException.class, () -> service.cancelar(1L, new CancelarCitaRequest()));
        }
        verify(citaRepository, never()).save(any());
    }

    @Test
    void rechazaResultadosAntesDeLaHoraProgramada() {
        Cita cita = new Cita();
        cita.setEstadoCita(new EstadoCita(1, "Programada"));
        cita.setFecha(LocalDateTime.now().plusHours(1));
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        RegistrarResultadoCitaRequest resultado = new RegistrarResultadoCitaRequest();
        resultado.setResultado("ATENDIDA");
        assertThrows(IllegalArgumentException.class, () -> service.registrarResultado(1L, resultado));
        resultado.setResultado("INASISTENCIA");
        assertThrows(IllegalArgumentException.class, () -> service.registrarResultado(1L, resultado));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void registraResultadoCuandoLaCitaPendienteYaOcurrio() {
        Cita cita = cita(1L);
        cita.setEstadoCita(new EstadoCita(3, "Reprogramada"));
        cita.setFecha(LocalDateTime.now().minusMinutes(1));
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(estadoCitaRepository.findById(5)).thenReturn(Optional.of(new EstadoCita(5, "Inasistencia")));
        when(citaRepository.save(cita)).thenReturn(cita);
        RegistrarResultadoCitaRequest resultado = new RegistrarResultadoCitaRequest();
        resultado.setResultado("INASISTENCIA");

        assertEquals(5, service.registrarResultado(1L, resultado).getIdEstadoCita());
        verify(citaRepository).save(cita);
    }

    @Test
    void indicaCasoExistenteAunqueLaCitaSeaAnteriorAlCaso() {
        Cita citaAnterior = cita(1L);
        Cita inicial = cita(2L);
        Caso caso = new Caso();
        caso.setId(5L);
        caso.setCodigo("CAS-5");
        caso.setCita(inicial);
        when(citaRepository.findAll()).thenReturn(List.of(citaAnterior, inicial));
        when(casoRepository.findFirstByCitaSolicitudAtencionIdOrderByFechaCreacionAsc(10L))
                .thenReturn(Optional.of(caso));

        List<CitaResponse> respuestas = service.listarTodasLasCitas();
        assertTrue(respuestas.get(0).isSolicitudTieneCaso());
        assertNull(respuestas.get(0).getCasoId());
        assertEquals("INICIAL", respuestas.get(0).getTipoAtencion());
        assertEquals(5L, respuestas.get(1).getCasoId());
        assertEquals("INICIAL", respuestas.get(1).getTipoAtencion());
    }

    private Cita cita(Long id) {
        Persona persona = new Persona();
        persona.setPrimerNombre("Persona");
        persona.setPrimerApellido("Prueba");
        SolicitudAtencion solicitud = new SolicitudAtencion();
        solicitud.setId(10L);
        solicitud.setSolicitante(persona);
        Cita cita = new Cita();
        cita.setId(id);
        cita.setSolicitudAtencion(solicitud);
        cita.setFecha(LocalDateTime.now().minusMinutes(1));
        cita.setEstadoCita(new EstadoCita(1, "Programada"));
        return cita;
    }
}

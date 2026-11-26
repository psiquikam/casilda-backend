package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.CasoListResponse;
import co.edu.udea.casilda.dto.request.DiscapacidadPersonaRequest;
import co.edu.udea.casilda.dto.request.PestanaDatosPersonaRequest;
import co.edu.udea.casilda.dto.request.PestanaDocumentacionRequest;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.ActividadMisional;
import co.edu.udea.casilda.model.entity.Cita;
import co.edu.udea.casilda.model.entity.DiscapacidadPersona;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.model.entity.Remision;
import co.edu.udea.casilda.model.entity.SolicitudAtencion;
import co.edu.udea.casilda.model.entity.SubTipoDiscapacidad;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.AtencionRepository;
import co.edu.udea.casilda.repository.ActividadMisionalRepository;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.PersonaRepository;
import co.edu.udea.casilda.repository.SubTipoDiscapacidadRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
class CasoServiceTest {

    @Mock
    private CasoRepository casoRepository;

    @Mock
    private AtencionRepository atencionRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaCasoService auditoriaCasoService;

    @Mock
    private SubTipoDiscapacidadRepository subTipoDiscapacidadRepository;

    @Mock
    private ActividadMisionalRepository actividadMisionalRepository;

    @InjectMocks
    private CasoService casoService;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listarCasosIncluyeCantidadDeAtencionesRegistradas() {
        Caso primero = new Caso();
        primero.setId(14L);
        Caso segundo = new Caso();
        segundo.setId(16L);

        when(casoRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(primero, segundo)));
        when(atencionRepository.countByCasoId(14L)).thenReturn(1L);
        when(atencionRepository.countByCasoId(16L)).thenReturn(3L);

        Page<CasoListResponse> resultado = casoService.listarCasosPaginados(0, 10);

        assertEquals(1L, resultado.getContent().get(0).getCantidadAtenciones());
        assertEquals(3L, resultado.getContent().get(1).getCantidadAtenciones());
    }

    @Test
    void guardaYRecuperaRelacionMisionalYAmbito() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("profesional", "test", List.of()));
        when(usuarioRepository.findByEmail("profesional")).thenReturn(Optional.of(new Usuario()));
        Caso caso = new Caso();
        caso.setId(14L);
        ActividadMisional actividad = new ActividadMisional();
        actividad.setId(8);
        when(casoRepository.findById(14L)).thenReturn(Optional.of(caso));
        when(casoRepository.save(caso)).thenReturn(caso);
        when(actividadMisionalRepository.findById(8)).thenReturn(Optional.of(actividad));

        PestanaDocumentacionRequest request = new PestanaDocumentacionRequest();
        request.setIdCaso(14L);
        request.setIdactivadmisional(8);
        request.setAmbitoOcurrencia("Otro");
        request.setAmbitoOcurrenciaOtro("Comunidad");
        casoService.registrarPestanaDocumentacion(request);

        CasoListResponse resultado = casoService.obtenerCasoPorId(14L);
        assertEquals(8, resultado.getIdActividadMisional());
        assertEquals("Otro", resultado.getAmbitoOcurrencia());
        assertEquals("Comunidad", resultado.getAmbitoOcurrenciaOtro());
        assertThrows(IllegalArgumentException.class, () -> casoService.actualizarAmbitoOcurrencia(caso, "Inexistente", null));
    }

    @Test
    void registraDiscapacidadesEnSolicitanteNoEnRemitente() {
        Persona solicitante = new Persona();
        solicitante.setId(1L);
        Persona remitente = new Persona();
        remitente.setId(2L);
        DiscapacidadPersona discapacidadRemitente = new DiscapacidadPersona();
        remitente.getDiscapacidades().add(discapacidadRemitente);
        prepararCaso(solicitante, remitente);

        SubTipoDiscapacidad primero = new SubTipoDiscapacidad();
        primero.setId(3);
        SubTipoDiscapacidad segundo = new SubTipoDiscapacidad();
        segundo.setId(4);
        when(subTipoDiscapacidadRepository.findById(3)).thenReturn(Optional.of(primero));
        when(subTipoDiscapacidadRepository.findById(4)).thenReturn(Optional.of(segundo));
        when(casoRepository.save(any(Caso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PestanaDatosPersonaRequest request = new PestanaDatosPersonaRequest();
        request.setIdCaso(10L);
        request.setDiscapacidades(List.of(discapacidad(3), discapacidad(4)));
        casoService.registrarPestanaDatosPersona(request);

        assertEquals(List.of(3, 4), solicitante.getDiscapacidades().stream()
                .map(DiscapacidadPersona::getIdsubtipodiscapacidad).toList());
        assertEquals(List.of(discapacidadRemitente), remitente.getDiscapacidades());
        verify(personaRepository).saveAndFlush(solicitante);
        verify(personaRepository, never()).saveAndFlush(remitente);
    }

    @Test
    void rechazaSubtipoInexistenteSinBorrarDiscapacidades() {
        Persona solicitante = new Persona();
        solicitante.setId(1L);
        DiscapacidadPersona existente = new DiscapacidadPersona();
        solicitante.getDiscapacidades().add(existente);
        prepararCaso(solicitante, new Persona());
        when(subTipoDiscapacidadRepository.findById(99)).thenReturn(Optional.empty());

        PestanaDatosPersonaRequest request = new PestanaDatosPersonaRequest();
        request.setIdCaso(10L);
        request.setDiscapacidades(List.of(discapacidad(99)));
        assertThrows(ResourceNotFoundException.class, () -> casoService.registrarPestanaDatosPersona(request));
        assertEquals(List.of(existente), solicitante.getDiscapacidades());
        verify(personaRepository, never()).saveAndFlush(any(Persona.class));
    }

    private void prepararCaso(Persona solicitante, Persona remitente) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("profesional", "test", List.of()));
        when(usuarioRepository.findByEmail("profesional")).thenReturn(Optional.of(new Usuario()));
        Remision remision = new Remision();
        remision.setRemitente(remitente);
        SolicitudAtencion solicitud = new SolicitudAtencion();
        solicitud.setSolicitante(solicitante);
        solicitud.setRemision(remision);
        Cita cita = new Cita();
        cita.setSolicitudAtencion(solicitud);
        Caso caso = new Caso();
        caso.setId(10L);
        caso.setCita(cita);
        when(casoRepository.findById(10L)).thenReturn(Optional.of(caso));
    }

    private DiscapacidadPersonaRequest discapacidad(int id) {
        DiscapacidadPersonaRequest request = new DiscapacidadPersonaRequest();
        request.setIdSubTipoDiscapacidad(id);
        return request;
    }
}

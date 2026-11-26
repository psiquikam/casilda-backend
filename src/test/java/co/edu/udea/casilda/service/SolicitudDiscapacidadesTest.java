package co.edu.udea.casilda.service;

import co.edu.udea.casilda.model.entity.DiscapacidadPersona;
import co.edu.udea.casilda.model.entity.EstadoSolicitud;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.model.entity.Remision;
import co.edu.udea.casilda.model.entity.SolicitudAtencion;
import co.edu.udea.casilda.model.entity.TipoSolicitud;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.PresuntoAgresorRepository;
import co.edu.udea.casilda.repository.SolicitudAtencionRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudDiscapacidadesTest {
    @Mock private SolicitudAtencionRepository solicitudes;
    @Mock private PresuntoAgresorRepository agresores;
    @Mock private UsuarioRepository usuarios;
    @InjectMocks private SolicitudAcompanamientoService service;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void profesionalConsultaDiscapacidadesDelSolicitanteNoDelRemitente() {
        autenticar("PROFESIONAL");
        SolicitudAtencion solicitud = solicitud();
        when(solicitudes.findById(1L)).thenReturn(Optional.of(solicitud));
        when(agresores.findBySolicitudAtencionId(1L)).thenReturn(Optional.empty());

        var resultado = service.obtenerPorId(1L).getDiscapacidades();

        assertEquals(1, resultado.size());
        assertEquals(3, resultado.get(0).getIdSubTipoDiscapacidad());
    }

    @Test
    void remitenteSinRolDeAtencionNoConsultaDiscapacidades() {
        autenticar("USUARIO");
        SolicitudAtencion solicitud = solicitud();
        Usuario creador = new Usuario();
        creador.setId(7L);
        solicitud.setUsuarioCreacion(creador);
        when(usuarios.findByEmail("cuenta")).thenReturn(Optional.of(creador));
        when(solicitudes.findById(1L)).thenReturn(Optional.of(solicitud));
        when(agresores.findBySolicitudAtencionId(1L)).thenReturn(Optional.empty());

        assertNull(service.obtenerPorId(1L).getDiscapacidades());
    }

    private void autenticar(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "cuenta", "test", List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private SolicitudAtencion solicitud() {
        Persona solicitante = new Persona();
        DiscapacidadPersona discapacidad = new DiscapacidadPersona();
        discapacidad.setIdsubtipodiscapacidad(3);
        solicitante.getDiscapacidades().add(discapacidad);
        Persona remitente = new Persona();
        DiscapacidadPersona discapacidadRemitente = new DiscapacidadPersona();
        discapacidadRemitente.setIdsubtipodiscapacidad(4);
        remitente.getDiscapacidades().add(discapacidadRemitente);
        Remision remision = new Remision();
        remision.setRemitente(remitente);
        SolicitudAtencion solicitud = new SolicitudAtencion();
        solicitud.setId(1L);
        solicitud.setTipoSolicitud(new TipoSolicitud());
        solicitud.setEstadoSolicitud(new EstadoSolicitud());
        solicitud.setSolicitante(solicitante);
        solicitud.setRemision(remision);
        return solicitud;
    }
}

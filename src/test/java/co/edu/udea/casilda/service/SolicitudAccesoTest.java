package co.edu.udea.casilda.service;

import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.SolicitudAtencion;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.ContactoTelefonicoRepository;
import co.edu.udea.casilda.repository.SolicitudAtencionRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** D-UAD-16: quien no es del equipo de atención solo lee las solicitudes que radicó. */
@ExtendWith(MockitoExtension.class)
class SolicitudAccesoTest {

    private static final long USUARIO_ID = 7L;

    @Mock
    private SolicitudAtencionRepository solicitudes;

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private ContactoTelefonicoRepository contactos;

    @InjectMocks
    private SolicitudAcompanamientoService service;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static Authentication con(final String... roles) {
        return new UsernamePasswordAuthenticationToken("persona@udea.edu.co", "x",
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
    }

    private void sesion(final String... roles) {
        SecurityContextHolder.getContext().setAuthentication(con(roles));
    }

    private void usuarioRegistrado() {
        final Usuario u = new Usuario();
        u.setId(USUARIO_ID);
        when(usuarios.findByEmail("persona@udea.edu.co")).thenReturn(Optional.of(u));
    }

    private SolicitudAtencion solicitudDe(final Long usuarioId) {
        final SolicitudAtencion s = new SolicitudAtencion();
        s.setId(50L);
        if (usuarioId != null) {
            final Usuario creador = new Usuario();
            creador.setId(usuarioId);
            s.setUsuarioCreacion(creador);
        }
        when(solicitudes.findById(50L)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void soloElEquipoDeAtencionVeTodasLasSolicitudes() {
        assertTrue(SolicitudAcceso.soloPropias(null));
        assertTrue(SolicitudAcceso.soloPropias(new AnonymousAuthenticationToken("k", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))));
        assertTrue(SolicitudAcceso.soloPropias(con("ROLE_USUARIO")));
        // Un rol ajeno al equipo de atención no amplía el alcance.
        assertTrue(SolicitudAcceso.soloPropias(con("ROLE_USUARIO", "ROLE_UAD_EQUIPO_3", "ROLE_GESTOR_CONTENIDO")));
        for (String rol : List.of("ROLE_ADMIN", "ROLE_COORDINADOR", "ROLE_PROFESIONAL", "ROLE_REVISOR")) {
            assertFalse(SolicitudAcceso.soloPropias(con("ROLE_USUARIO", rol)), rol);
        }
    }

    @Test
    void elRolUsuarioListaSoloLoQueRadico() {
        sesion("ROLE_USUARIO");
        usuarioRegistrado();
        when(solicitudes.findByUsuarioCreacionIdOrderByFechaCreacionDesc(eq(USUARIO_ID), any(Pageable.class))).thenReturn(Page.empty());
        when(solicitudes.findByUsuarioCreacionIdAndEstadoSolicitudIdOrderByFechaCreacionDesc(USUARIO_ID, 2)).thenReturn(List.of());

        service.listarPaginadas(0, 10, null, false);
        service.listarTodas(2);

        verify(solicitudes).findByUsuarioCreacionIdOrderByFechaCreacionDesc(eq(USUARIO_ID), any(Pageable.class));
        verify(solicitudes).findByUsuarioCreacionIdAndEstadoSolicitudIdOrderByFechaCreacionDesc(USUARIO_ID, 2);
        verify(solicitudes, never()).findAll();
        verify(solicitudes, never()).findAllByOrderByFechaCreacionDesc(any());
        verify(solicitudes, never()).findByEstadoSolicitudIdOrderByFechaCreacionDesc(any());
    }

    @Test
    void elEquipoDeAtencionSigueListandoTodo() {
        sesion("ROLE_PROFESIONAL");
        when(solicitudes.findAllByOrderByFechaCreacionDesc(any())).thenReturn(Page.empty());

        service.listarPaginadas(0, 10, null, false);

        verify(solicitudes).findAllByOrderByFechaCreacionDesc(any());
        verify(solicitudes, never()).findByUsuarioCreacionIdOrderByFechaCreacionDesc(anyLong(), any(Pageable.class));
    }

    @Test
    void ordenRepartoNoAmpliaElAccesoDelUsuario() {
        sesion("ROLE_USUARIO");
        usuarioRegistrado();
        when(solicitudes.findByUsuarioCreacionIdOrderByFechaCreacionDesc(eq(USUARIO_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listarPaginadas(0, 10, null, true);

        verify(solicitudes).findByUsuarioCreacionIdOrderByFechaCreacionDesc(eq(USUARIO_ID), any(Pageable.class));
        verify(solicitudes, never()).findParaReparto(any(), any(), any());
    }

    @Test
    void equipoDeAtencionPuedeUsarElOrdenDeReparto() {
        sesion("ROLE_PROFESIONAL");
        when(solicitudes.findParaReparto(any(), any(), any())).thenReturn(Page.empty());

        service.listarPaginadas(0, 10, null, true);

        verify(solicitudes).findParaReparto(eq(null), eq(1), any(Pageable.class));
        verify(solicitudes, never()).findByUsuarioCreacionIdOrderByFechaCreacionDesc(anyLong(), any(Pageable.class));
    }

    @Test
    void solicitudAjenaRespondeComoInexistente() {
        sesion("ROLE_USUARIO");
        usuarioRegistrado();
        solicitudDe(99L);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorId(50L));
        assertThrows(ResourceNotFoundException.class, () -> service.listarContactos(50L));
        verify(contactos, never()).findBySolicitudAtencionIdOrderByFechaCreacionDesc(any());
    }

    @Test
    void solicitudSinCreadorRegistradoTampocoSeEntregaAlRolUsuario() {
        sesion("ROLE_USUARIO");
        usuarioRegistrado();
        solicitudDe(null);

        assertThrows(ResourceNotFoundException.class, () -> service.listarContactos(50L));
    }

    @Test
    void laPersonaVeLosContactosDeSuPropiaSolicitud() {
        sesion("ROLE_USUARIO");
        usuarioRegistrado();
        solicitudDe(USUARIO_ID);
        when(contactos.findBySolicitudAtencionIdOrderByFechaCreacionDesc(50L)).thenReturn(List.of());

        assertTrue(service.listarContactos(50L).isEmpty());
    }
}

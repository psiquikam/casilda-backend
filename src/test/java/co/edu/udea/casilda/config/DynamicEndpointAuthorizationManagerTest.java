package co.edu.udea.casilda.config;

import co.edu.udea.casilda.model.entity.Endpoint;
import co.edu.udea.casilda.model.entity.EndpointRole;
import co.edu.udea.casilda.model.entity.Rol;
import co.edu.udea.casilda.repository.EndpointRepository;
import co.edu.udea.casilda.repository.EndpointRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicEndpointAuthorizationManagerTest {
    @Test
    void ultimaCitaReutilizaPermisosDeContactosYRechazaAccesoAnonimo() {
        EndpointRepository endpoints = mock(EndpointRepository.class);
        EndpointRoleRepository roles = mock(EndpointRoleRepository.class);
        Endpoint endpoint = new Endpoint();
        endpoint.setId(1L);
        endpoint.setPath("/solicitudes/acompanamiento/*/contactos");
        endpoint.setHttpMethod("GET");
        Rol profesional = new Rol(1, "PROFESIONAL");
        EndpointRole permiso = new EndpointRole();
        permiso.setRol(profesional);
        when(endpoints.findByActivoTrue()).thenReturn(List.of(endpoint));
        when(roles.findByEndpointId(1L)).thenReturn(List.of(permiso));
        var manager = new DynamicEndpointAuthorizationManager(endpoints, roles);
        var contexto = new RequestAuthorizationContext(new MockHttpServletRequest(), Map.of("solicitudId", "50"));
        var permitido = new UsernamePasswordAuthenticationToken("profesional", "x",
                List.of(new SimpleGrantedAuthority("ROLE_PROFESIONAL")));
        var denegado = new UsernamePasswordAuthenticationToken("otro", "x",
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));

        assertTrue(manager.checkUltimaCita(() -> permitido, contexto).isGranted());
        assertFalse(manager.checkUltimaCita(() -> denegado, contexto).isGranted());
        assertFalse(manager.checkUltimaCita(() -> null, contexto).isGranted());
    }
}

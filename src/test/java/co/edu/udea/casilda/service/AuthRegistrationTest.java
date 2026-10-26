package co.edu.udea.casilda.service;

import co.edu.udea.casilda.config.JwtService;
import co.edu.udea.casilda.dto.request.RegistroUsuarioRequest;
import co.edu.udea.casilda.exception.DuplicateResourceException;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.model.entity.Rol;
import co.edu.udea.casilda.model.entity.TipoIdentificacion;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.model.entity.VinculoUdeA;
import co.edu.udea.casilda.repository.CorreoPersonaRepository;
import co.edu.udea.casilda.repository.PersonaRepository;
import co.edu.udea.casilda.repository.RoleRepository;
import co.edu.udea.casilda.repository.TelefonoPersonaRepository;
import co.edu.udea.casilda.repository.TipoIdentificacionRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import co.edu.udea.casilda.repository.VinculoUdeARepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRegistrationTest {

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private TipoIdentificacionRepository tipoIdentificacionRepository;
    @Mock
    private VinculoUdeARepository vinculoUdeARepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CorreoPersonaRepository correoPersonaRepository;
    @Mock
    private TelefonoPersonaRepository telefonoPersonaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registrationReusesExistingPersonaWithoutModifyingIt() {
        RegistroUsuarioRequest request = request();
        request.setVinculoUdeaId(null);
        TipoIdentificacion tipo = tipoIdentificacion();
        Persona persona = new Persona();
        persona.setId(12L);
        persona.setPrimerNombre("Nombre original");
        Rol rol = new Rol();
        rol.setCodigo("USUARIO");

        when(usuarioRepository.findByEmailIgnoreCase("persona@example.com")).thenReturn(Optional.empty());
        when(tipoIdentificacionRepository.findByCodigo("CC")).thenReturn(Optional.of(tipo));
        when(roleRepository.findByCodigo("USUARIO")).thenReturn(Optional.of(rol));
        when(personaRepository.findByNumeroDocumentoAndTipoIdentificacion_Id("12345678", 2))
                .thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByPersona_Id(12L)).thenReturn(false);
        when(passwordEncoder.encode("ContraseñaSegura123")).thenReturn("encoded-password");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(34L);
            return usuario;
        });

        var response = authService.registrar(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuario = usuarioCaptor.getValue();
        assertEquals(persona, usuario.getPersona());
        assertEquals("Nombre original", persona.getPrimerNombre());
        assertEquals("encoded-password", usuario.getPassword());
        assertTrue(usuario.getHabeasData());
        assertNotNull(usuario.getFechaHabeasData());
        assertEquals(34L, response.getUsuarioId());
        verify(personaRepository, never()).save(any(Persona.class));
        verify(correoPersonaRepository, never()).save(any());
        verify(telefonoPersonaRepository, never()).save(any());
        verify(vinculoUdeARepository, never()).findById(any());
    }

    @Test
    void registrationCreatesPersonaAndContactsWhenNoMatchExists() {
        RegistroUsuarioRequest request = request();
        TipoIdentificacion tipo = tipoIdentificacion();
        VinculoUdeA vinculo = new VinculoUdeA();
        vinculo.setId(1);
        Rol rol = new Rol();
        rol.setCodigo("USUARIO");

        when(usuarioRepository.findByEmailIgnoreCase("persona@example.com")).thenReturn(Optional.empty());
        when(tipoIdentificacionRepository.findByCodigo("CC")).thenReturn(Optional.of(tipo));
        when(roleRepository.findByCodigo("USUARIO")).thenReturn(Optional.of(rol));
        when(personaRepository.findByNumeroDocumentoAndTipoIdentificacion_Id("12345678", 2))
                .thenReturn(Optional.empty());
        when(vinculoUdeARepository.findById(1)).thenReturn(Optional.of(vinculo));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(12L);
            return persona;
        });
        when(usuarioRepository.existsByPersona_Id(12L)).thenReturn(false);
        when(passwordEncoder.encode("ContraseñaSegura123")).thenReturn("encoded-password");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(34L);
            return usuario;
        });

        authService.registrar(request);

        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(personaCaptor.capture());
        assertEquals(vinculo, personaCaptor.getValue().getVinculoUdea());
        verify(correoPersonaRepository).save(any());
        verify(telefonoPersonaRepository).save(any());
    }

    @Test
    void registrationRejectsPersonaAlreadyLinkedToAUser() {
        RegistroUsuarioRequest request = request();
        TipoIdentificacion tipo = tipoIdentificacion();
        Persona persona = new Persona();
        persona.setId(12L);
        Rol rol = new Rol();
        rol.setCodigo("USUARIO");

        when(usuarioRepository.findByEmailIgnoreCase("persona@example.com")).thenReturn(Optional.empty());
        when(tipoIdentificacionRepository.findByCodigo("CC")).thenReturn(Optional.of(tipo));
        when(roleRepository.findByCodigo("USUARIO")).thenReturn(Optional.of(rol));
        when(personaRepository.findByNumeroDocumentoAndTipoIdentificacion_Id("12345678", 2))
                .thenReturn(Optional.of(persona));
        when(usuarioRepository.existsByPersona_Id(12L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.registrar(request));
        verify(usuarioRepository, never()).save(any());
    }

    private RegistroUsuarioRequest request() {
        RegistroUsuarioRequest request = new RegistroUsuarioRequest();
        request.setPrimerNombre("Ana");
        request.setPrimerApellido("Pérez");
        request.setSegundoApellido("Gómez");
        request.setTipoDocumento("CC");
        request.setDocumento("12345678");
        request.setVinculoUdeaId(1);
        request.setEmail("persona@example.com");
        request.setTelefono("3001234567");
        request.setPassword("ContraseñaSegura123");
        request.setHabeasData(true);
        return request;
    }

    private TipoIdentificacion tipoIdentificacion() {
        TipoIdentificacion tipo = new TipoIdentificacion();
        tipo.setId(2);
        tipo.setCodigo("CC");
        return tipo;
    }
}

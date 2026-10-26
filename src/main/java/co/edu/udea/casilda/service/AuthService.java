package co.edu.udea.casilda.service;

import co.edu.udea.casilda.config.JwtService;
import co.edu.udea.casilda.dto.request.AuthLoginRequest;
import co.edu.udea.casilda.dto.request.RegistroUsuarioRequest;
import co.edu.udea.casilda.dto.response.AuthLoginResponse;
import co.edu.udea.casilda.dto.response.RegistroUsuarioResponse;
import co.edu.udea.casilda.exception.DuplicateResourceException;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.CorreoPersona;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.model.entity.Rol;
import co.edu.udea.casilda.model.entity.TelefonoPersona;
import co.edu.udea.casilda.model.entity.TipoIdentificacion;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.model.entity.VinculoUdeA;
import co.edu.udea.casilda.model.enums.TipoCorreoEnum;
import co.edu.udea.casilda.model.enums.TipoTelefonoEnum;
import co.edu.udea.casilda.repository.CorreoPersonaRepository;
import co.edu.udea.casilda.repository.PersonaRepository;
import co.edu.udea.casilda.repository.RoleRepository;
import co.edu.udea.casilda.repository.TelefonoPersonaRepository;
import co.edu.udea.casilda.repository.TipoIdentificacionRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import co.edu.udea.casilda.repository.VinculoUdeARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PersonaRepository personaRepository;
    private final TipoIdentificacionRepository tipoIdentificacionRepository;
    private final VinculoUdeARepository vinculoUdeARepository;
    private final RoleRepository roleRepository;
    private final CorreoPersonaRepository correoPersonaRepository;
    private final TelefonoPersonaRepository telefonoPersonaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegistroUsuarioResponse registrar(final RegistroUsuarioRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo ingresado.");
        }

        TipoIdentificacion tipoIdentificacion = tipoIdentificacionRepository.findByCodigo(
                        request.getTipoDocumento().trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de documento no encontrado: " + request.getTipoDocumento()));
        Rol rolUsuario = roleRepository.findByCodigo("USUARIO")
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el rol USUARIO."));

        Persona persona = personaRepository.findByNumeroDocumentoAndTipoIdentificacion_Id(
                        request.getDocumento().trim(), tipoIdentificacion.getId())
                .orElseGet(() -> crearPersona(request, tipoIdentificacion, email));

        if (usuarioRepository.existsByPersona_Id(persona.getId())) {
            throw new DuplicateResourceException("Ya existe un usuario registrado con ese documento.");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setNombre(construirNombre(persona));
        usuario.setPersona(persona);
        usuario.setRol(rolUsuario);
        usuario.setActivo(true);
        usuario.setHabeasData(true);
        usuario.setFechaHabeasData(LocalDateTime.now());

        Usuario usuarioCreado = usuarioRepository.save(usuario);
        log.info("Usuario registrado exitosamente con ID: {}", usuarioCreado.getId());
        return RegistroUsuarioResponse.builder()
                .mensaje("Usuario creado exitosamente.")
                .usuarioId(usuarioCreado.getId())
                .email(usuarioCreado.getEmail())
                .build();
    }

    private Persona crearPersona(
            RegistroUsuarioRequest request,
            TipoIdentificacion tipoIdentificacion,
            String email
    ) {
        if (request.getVinculoUdeaId() == null) {
            throw new IllegalArgumentException("El vínculo con la UdeA es obligatorio para crear una persona.");
        }
        VinculoUdeA vinculoUdeA = vinculoUdeARepository.findById(request.getVinculoUdeaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vínculo con la UdeA no encontrado: " + request.getVinculoUdeaId()));

        Persona persona = new Persona();
        persona.setPrimerNombre(request.getPrimerNombre().trim());
        persona.setSegundoNombre(normalizarOpcional(request.getSegundoNombre()));
        persona.setPrimerApellido(request.getPrimerApellido().trim());
        persona.setSegundoApellido(normalizarOpcional(request.getSegundoApellido()));
        persona.setNumeroDocumento(request.getDocumento().trim());
        persona.setTipoIdentificacion(tipoIdentificacion);
        persona.setVinculoUdea(vinculoUdeA);
        Persona personaCreada = personaRepository.save(persona);

        CorreoPersona correo = new CorreoPersona();
        correo.setIdpersona(personaCreada.getId());
        correo.setIdtipo(email.endsWith("@udea.edu.co")
                ? TipoCorreoEnum.INSTITUCIONAL.getId()
                : TipoCorreoEnum.PERSONAL.getId());
        correo.setCorreo(email);
        correoPersonaRepository.save(correo);

        TelefonoPersona telefono = new TelefonoPersona();
        telefono.setIdpersona(personaCreada.getId());
        telefono.setIdtipo(TipoTelefonoEnum.CELULAR.getId());
        telefono.setTelefono(request.getTelefono());
        telefonoPersonaRepository.save(telefono);
        return personaCreada;
    }

    private String construirNombre(Persona persona) {
        return Stream.of(
                        persona.getPrimerNombre(),
                        persona.getSegundoNombre(),
                        persona.getPrimerApellido(),
                        persona.getSegundoApellido())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private String normalizarOpcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public AuthLoginResponse login(final AuthLoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String token = jwtService.generateToken(userDetails);
        Set<String> roles = usuario.getRoles().stream()
                .map(rol -> rol.getCodigo() == null || rol.getCodigo().isBlank()
                        ? rol.getNombre()
                        : rol.getCodigo())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roles.isEmpty() && usuario.getRol() != null) {
            roles.add(usuario.getRol().getCodigo() == null || usuario.getRol().getCodigo().isBlank()
                    ? usuario.getRol().getNombre()
                    : usuario.getRol().getCodigo());
        }
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return AuthLoginResponse.builder()
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol() == null ? null : usuario.getRol().getNombre())
                .roles(roles.stream().toList())
                .authorities(authorities)
                .foto(null)
                .token(token)
                .build();
    }
}

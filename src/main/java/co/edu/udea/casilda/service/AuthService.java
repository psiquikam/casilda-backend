package co.edu.udea.casilda.service;

import co.edu.udea.casilda.config.JwtService;
import co.edu.udea.casilda.dto.request.AuthLoginRequest;
import co.edu.udea.casilda.dto.response.AuthLoginResponse;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

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

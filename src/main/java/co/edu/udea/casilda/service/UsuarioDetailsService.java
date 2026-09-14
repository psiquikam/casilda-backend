package co.edu.udea.casilda.service;

import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String username) {
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        Set<String> roleNames = usuario.getRoles().stream()
                .filter(rol -> Boolean.TRUE.equals(rol.getActivo()))
                .map(rol -> rol.getCodigo() == null || rol.getCodigo().isBlank()
                        ? rol.getNombre()
                        : rol.getCodigo())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleNames.isEmpty() && usuario.getRol() != null) {
            roleNames.add(usuario.getRol().getCodigo() == null || usuario.getRol().getCodigo().isBlank()
                    ? usuario.getRol().getNombre().toUpperCase()
                    : usuario.getRol().getCodigo().toUpperCase());
        }

        List<GrantedAuthority> authorities = roleNames.stream()
                .map(roleName -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + roleName))
                .toList();

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(authorities)
                .disabled(Boolean.FALSE.equals(usuario.getActivo()))
                .build();
    }
}

package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.UsuarioUpsertRequest;
import co.edu.udea.casilda.dto.response.UsuarioResponse;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.Rol;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.RoleRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio para gestión CRUD de usuarios
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Obtiene todos los usuarios del sistema
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> obtenerTodos() {
        log.info("Obteniendo todos los usuarios");
        return usuarioRepository.findAll().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene usuarios con paginación
     */
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> obtenerPaginados(final int page, final int size) {
        log.info("Obteniendo usuarios paginados. page={}, size={}", page, size);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        return usuarioRepository.findAll(pageable).map(this::convertirAResponse);
    }

    /**
     * Obtiene un usuario por su ID
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(final Long id) {
        log.info("Obteniendo usuario con ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return convertirAResponse(usuario);
    }

    /**
     * Crea un nuevo usuario
     */
    @Transactional
    public UsuarioResponse crear(final UsuarioUpsertRequest request) {
        log.info("Creando nuevo usuario con email: {}", request.getEmail());
        
        // Verificar que el email no exista
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + request.getEmail());
        }

        // Verificar que la contraseña esté presente al crear
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria al crear un usuario");
        }

        Set<Rol> roles = obtenerRoles(request);

        // Crear el usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRoles(roles);
        usuario.setActivo(request.getActivo() != null ? request.getActivo() : true);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        log.info("Usuario creado exitosamente con ID: {}", usuarioGuardado.getId());
        
        return convertirAResponse(usuarioGuardado);
    }

    /**
     * Actualiza un usuario existente
     */
    @Transactional
    public UsuarioResponse actualizar(final Long id, final UsuarioUpsertRequest request) {
        log.info("Actualizando usuario con ID: {}", id);
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        // Verificar que el email no esté en uso por otro usuario
        usuarioRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            if (!u.getId().equals(id)) {
                throw new IllegalArgumentException("El email ya está en uso por otro usuario");
            }
        });

        Set<Rol> roles = obtenerRoles(request);

        // Actualizar campos
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setRoles(roles);
        
        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }

        // Solo actualizar la contraseña si se proporciona una nueva
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        log.info("Usuario actualizado exitosamente con ID: {}", usuarioActualizado.getId());
        
        return convertirAResponse(usuarioActualizado);
    }

    /**
     * Elimina un usuario por su ID
     */
    @Transactional
    public void eliminar(final Long id) {
        log.info("Eliminando usuario con ID: {}", id);
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        
        usuarioRepository.delete(usuario);
        log.info("Usuario eliminado exitosamente con ID: {}", id);
    }

    /**
     * Activa o desactiva un usuario
     */
    @Transactional
    public UsuarioResponse cambiarEstado(final Long id, final Boolean activo) {
        log.info("Cambiando estado de usuario con ID: {} a activo: {}", id, activo);
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        
        usuario.setActivo(activo);
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        
        log.info("Estado del usuario actualizado exitosamente");
        return convertirAResponse(usuarioActualizado);
    }

    /**
     * Convierte una entidad Usuario a UsuarioResponse
     */
    private UsuarioResponse convertirAResponse(final Usuario usuario) {
        Set<Rol> roles = usuario.getRoles();
        Rol rolPrincipal = usuario.getRol();
        if ((roles == null || roles.isEmpty()) && rolPrincipal != null) {
            roles = Set.of(rolPrincipal);
        }

        Set<Integer> idsRoles = roles == null
                ? Set.of()
                : roles.stream().map(Rol::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> codigosRoles = roles == null
                ? Set.of()
                : roles.stream().map(Rol::getCodigo).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> nombresRoles = roles == null
                ? Set.of()
                : roles.stream().map(Rol::getNombre).collect(Collectors.toCollection(LinkedHashSet::new));

        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .idRol(rolPrincipal == null ? null : rolPrincipal.getId())
                .nombreRol(rolPrincipal == null ? null : rolPrincipal.getNombre())
                .idsRoles(idsRoles)
                .codigosRoles(codigosRoles)
                .nombresRoles(nombresRoles)
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }

    private Set<Rol> obtenerRoles(final UsuarioUpsertRequest request) {
        Set<Integer> idsRoles = new LinkedHashSet<>();
        if (request.getIdsRoles() != null) {
            idsRoles.addAll(request.getIdsRoles());
        }
        if (idsRoles.isEmpty() && request.getIdRol() != null) {
            idsRoles.add(request.getIdRol());
        }
        if (idsRoles.isEmpty()) {
            throw new IllegalArgumentException("Al menos un rol es obligatorio");
        }

        List<Rol> rolesEncontrados = roleRepository.findAllById(idsRoles);
        Map<Integer, Rol> rolesPorId = rolesEncontrados.stream()
                .collect(Collectors.toMap(Rol::getId, rol -> rol));
        boolean faltaUnRol = idsRoles.stream().anyMatch(idRol -> !rolesPorId.containsKey(idRol));
        if (faltaUnRol) {
            Integer idRol = idsRoles.stream()
                    .filter(id -> !rolesPorId.containsKey(id))
                    .findFirst()
                    .orElseThrow();
            throw new ResourceNotFoundException("Rol no encontrado con ID: " + idRol);
        }

        return idsRoles.stream()
                .map(rolesPorId::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

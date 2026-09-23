package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.ContenidoRequest;
import co.edu.udea.casilda.dto.response.ContenidoHomeResponse;
import co.edu.udea.casilda.dto.response.ContenidoResponse;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.Contenido;
import co.edu.udea.casilda.model.enums.SeccionContenidoEnum;
import co.edu.udea.casilda.repository.ContenidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Servicio para gestionar los contenidos destacados del Home: la consulta
 * pública de contenidos vigentes y el CRUD administrativo del gestor de
 * contenidos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContenidoService {

    private final ContenidoRepository contenidoRepository;

    /**
     * Lista los contenidos vigentes al momento de la consulta, para el
     * endpoint público del Home. El filtrado de vigencia es responsabilidad
     * exclusiva del backend: nunca se exponen contenidos futuros, expirados
     * o eliminados lógicamente.
     */
    @Transactional(readOnly = true)
    public List<ContenidoHomeResponse> listarVigentesHome() {
        log.info("Listando contenidos vigentes del Home");
        Instant ahora = Instant.now();
        return contenidoRepository.findVigentes(ahora).stream()
                .map(this::toHomeResponse)
                .toList();
    }

    /**
     * Lista paginada de contenidos (vigentes, futuros y expirados, pero no
     * eliminados) para la tabla de administración.
     */
    @Transactional(readOnly = true)
    public Page<ContenidoResponse> listarPaginado(final int page, final int size) {
        log.info("Listando contenidos paginados: page={}, size={}", page, size);
        return contenidoRepository
                .findByEliminadoFalseOrderByOrdenAscTituloAsc(PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ContenidoResponse obtenerPorId(final Long id) {
        log.info("Obteniendo contenido con ID: {}", id);
        return toResponse(buscarActivoPorId(id));
    }

    @Transactional
    public ContenidoResponse crear(final ContenidoRequest request) {
        log.info("Creando contenido: {}", request.getTitulo());
        validarVigencia(request);

        Contenido contenido = new Contenido();
        aplicarCampos(contenido, request);
        Contenido guardado = contenidoRepository.save(contenido);
        return toResponse(guardado);
    }

    @Transactional
    public ContenidoResponse actualizar(final Long id, final ContenidoRequest request) {
        log.info("Actualizando contenido con ID: {}", id);
        validarVigencia(request);

        Contenido contenido = buscarActivoPorId(id);
        aplicarCampos(contenido, request);
        Contenido guardado = contenidoRepository.save(contenido);
        return toResponse(guardado);
    }

    /**
     * Borrado lógico: nunca se elimina físicamente, por trazabilidad. No
     * requiere motivo de despublicación.
     */
    @Transactional
    public void eliminar(final Long id) {
        log.info("Eliminando (lógicamente) contenido con ID: {}", id);
        Contenido contenido = buscarActivoPorId(id);
        contenido.setEliminado(true);
        contenidoRepository.save(contenido);
    }

    private Contenido buscarActivoPorId(final Long id) {
        return contenidoRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado con ID: " + id));
    }

    private void validarVigencia(final ContenidoRequest request) {
        if (request.getVigenciaFin() != null
                && !request.getVigenciaFin().isAfter(request.getVigenciaInicio())) {
            throw new IllegalArgumentException("vigenciaFin debe ser posterior a vigenciaInicio");
        }
    }

    private void aplicarCampos(final Contenido contenido, final ContenidoRequest request) {
        contenido.setImagen(request.getImagen());
        contenido.setTitulo(request.getTitulo());
        contenido.setContenido(request.getContenido());
        contenido.setVigenciaInicio(request.getVigenciaInicio());
        contenido.setVigenciaFin(request.getVigenciaFin());
        contenido.setEnlace(request.getEnlace());
        contenido.setSeccion(SeccionContenidoEnum.valueOf(request.getSeccion().toUpperCase(Locale.ROOT)));
        contenido.setOrden(request.getOrden() == null ? 0 : request.getOrden());
    }

    private ContenidoHomeResponse toHomeResponse(final Contenido contenido) {
        return ContenidoHomeResponse.builder()
                .id(contenido.getId())
                .imagen(contenido.getImagen())
                .titulo(contenido.getTitulo())
                .contenido(contenido.getContenido())
                .vigenciaInicio(contenido.getVigenciaInicio())
                .vigenciaFin(contenido.getVigenciaFin())
                .enlace(contenido.getEnlace())
                .seccion(contenido.getSeccion().name().toLowerCase(Locale.ROOT))
                .build();
    }

    private ContenidoResponse toResponse(final Contenido contenido) {
        return ContenidoResponse.builder()
                .id(contenido.getId())
                .imagen(contenido.getImagen())
                .titulo(contenido.getTitulo())
                .contenido(contenido.getContenido())
                .vigenciaInicio(contenido.getVigenciaInicio())
                .vigenciaFin(contenido.getVigenciaFin())
                .enlace(contenido.getEnlace())
                .seccion(contenido.getSeccion().name().toLowerCase(Locale.ROOT))
                .orden(contenido.getOrden())
                .eliminado(contenido.getEliminado())
                .createdAt(contenido.getCreatedAt())
                .createdBy(contenido.getCreatedBy())
                .modifiedAt(contenido.getModifiedAt())
                .modifiedBy(contenido.getModifiedBy())
                .build();
    }
}

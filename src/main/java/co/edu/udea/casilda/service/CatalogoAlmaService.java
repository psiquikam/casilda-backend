package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.CatalogoAlmaItemResponse;
import co.edu.udea.casilda.exception.CatalogoAlmaNoEncontradoException;
import co.edu.udea.casilda.repository.CatalogoAlmaItemRepository;
import co.edu.udea.casilda.repository.TipoIdentificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogoAlmaService {
    private static final String TIPOS_IDENTIFICACION = "tipos-identificacion";
    private static final Set<String> CATALOGOS = Set.of(
            TIPOS_IDENTIFICACION, "estratos", "parentescos-solicitante", "destinos-internos",
            "rutas-externas", "resultados-ruta-externa", "motivos-cierre-aph", "textos-consentimiento");

    private final CatalogoAlmaItemRepository catalogoRepository;
    private final TipoIdentificacionRepository tipoIdentificacionRepository;

    @Transactional(readOnly = true)
    public List<CatalogoAlmaItemResponse> obtenerCatalogo(String nombre) {
        if (!CATALOGOS.contains(nombre)) {
            throw new CatalogoAlmaNoEncontradoException();
        }
        log.info("Consultando catálogo ALMA: {}", nombre);
        if (TIPOS_IDENTIFICACION.equals(nombre)) {
            return tipoIdentificacionRepository.findAll().stream()
                    .map(tipo -> CatalogoAlmaItemResponse.builder()
                            .codigo(tipo.getCodigo()).etiqueta(tipo.getNombre()).activo(tipo.getActivo()).build())
                    .toList();
        }
        return catalogoRepository.findByCatalogoOrderByIdAsc(nombre).stream()
                .map(item -> CatalogoAlmaItemResponse.builder()
                        .codigo(item.getCodigo()).etiqueta(item.getEtiqueta()).activo(item.getActivo()).build())
                .toList();
    }
}

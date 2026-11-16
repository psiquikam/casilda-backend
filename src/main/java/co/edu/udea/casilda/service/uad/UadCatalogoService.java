package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.response.MaestroDTO;
import co.edu.udea.casilda.model.enums.uad.CatalogoUad;
import co.edu.udea.casilda.repository.uad.UadCatalogoItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Catálogos UAD con la forma real de {@code MaestroDTO}. Valores PENDIENTES DE VALIDACIÓN jurídica. */
@Service
@RequiredArgsConstructor
public class UadCatalogoService {

    private final UadCatalogoItemRepository repository;

    @Transactional(readOnly = true)
    public List<MaestroDTO> listar(final CatalogoUad catalogo) {
        return repository.findByCatalogoAndActivoTrueOrderByOrdenAscNombreAsc(catalogo).stream()
                .map(i -> MaestroDTO.builder().id(i.getId()).codigo(i.getCodigo()).nombre(i.getNombre()).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existe(final CatalogoUad catalogo, final String codigo) {
        return codigo != null && repository.existsByCatalogoAndCodigoAndActivoTrue(catalogo, codigo);
    }

    @Transactional(readOnly = true)
    public String nombreDe(final CatalogoUad catalogo, final String codigo) {
        return repository.findByCatalogoAndActivoTrueOrderByOrdenAscNombreAsc(catalogo).stream()
                .filter(i -> i.getCodigo().equals(codigo))
                .map(co.edu.udea.casilda.model.entity.uad.UadCatalogoItem::getNombre)
                .findFirst()
                .orElse(codigo);
    }
}

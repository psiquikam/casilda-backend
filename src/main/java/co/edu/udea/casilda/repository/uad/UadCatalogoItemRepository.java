package co.edu.udea.casilda.repository.uad;

import co.edu.udea.casilda.model.entity.uad.UadCatalogoItem;
import co.edu.udea.casilda.model.enums.uad.CatalogoUad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UadCatalogoItemRepository extends JpaRepository<UadCatalogoItem, Long> {

    List<UadCatalogoItem> findByCatalogoAndActivoTrueOrderByOrdenAscNombreAsc(CatalogoUad catalogo);

    boolean existsByCatalogoAndCodigoAndActivoTrue(CatalogoUad catalogo, String codigo);
}

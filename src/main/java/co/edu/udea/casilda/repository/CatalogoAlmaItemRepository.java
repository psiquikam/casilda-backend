package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.CatalogoAlmaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoAlmaItemRepository extends JpaRepository<CatalogoAlmaItem, Long> {
    /** Obtiene los elementos de un catálogo en orden estable. */
    List<CatalogoAlmaItem> findByCatalogoOrderByIdAsc(String catalogo);
}

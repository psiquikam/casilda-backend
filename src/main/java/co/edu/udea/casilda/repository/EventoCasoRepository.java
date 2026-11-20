package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.EventoCaso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para consultar eventos de auditoría asociados a un caso.
 */
@Repository
public interface EventoCasoRepository extends JpaRepository<EventoCaso, Long> {

    /**
     * Lista los eventos de un caso en orden cronológico.
     */
    List<EventoCaso> findByCasoIdOrderByFechaAscIdAsc(Long casoId);
}

package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.Atencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio para gestionar Atencion
 */
@Repository
public interface AtencionRepository extends JpaRepository<Atencion, Long> {
    long countByCasoId(Long casoId);
    Optional<Atencion> findByCasoCitaId(Long citaId);
    Optional<Atencion> findByCasoId(Long casoId);
    Optional<Atencion> findByCasoIdAndCitaId(Long casoId, Long citaId);
    Optional<Atencion> findByCitaId(Long citaId);
}

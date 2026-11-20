package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.EventoAtencionAlma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoAtencionAlmaRepository extends JpaRepository<EventoAtencionAlma, Long> {

    /** Lista eventos de una atención APH en orden cronológico ascendente. */
    java.util.List<EventoAtencionAlma> findByAtencionCodigoOrderByFechaAscIdAsc(String codigo);
}

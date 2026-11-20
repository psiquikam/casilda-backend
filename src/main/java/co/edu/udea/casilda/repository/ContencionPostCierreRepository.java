package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.ContencionPostCierre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContencionPostCierreRepository extends JpaRepository<ContencionPostCierre, Long> {
    /** Lista las contenciones de una atención en orden de registro. */
    List<ContencionPostCierre> findByAtencionIdOrderByFechaRegistroAscIdAsc(Long atencionId);
}

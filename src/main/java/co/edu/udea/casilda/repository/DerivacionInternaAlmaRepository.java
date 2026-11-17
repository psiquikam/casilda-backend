package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.DerivacionInternaAlma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DerivacionInternaAlmaRepository extends JpaRepository<DerivacionInternaAlma, Long> {
    /** Lista las derivaciones de una atención en orden de registro. */
    List<DerivacionInternaAlma> findByAtencionIdOrderByIdAsc(Long atencionId);
}

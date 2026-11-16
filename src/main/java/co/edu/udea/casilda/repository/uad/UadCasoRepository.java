package co.edu.udea.casilda.repository.uad;

import co.edu.udea.casilda.model.entity.uad.UadCaso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UadCasoRepository extends JpaRepository<UadCaso, UUID> {

    boolean existsByCasoId(String casoId);
}

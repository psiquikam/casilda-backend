package co.edu.udea.casilda.repository.uad;

import co.edu.udea.casilda.model.entity.uad.UadSolicitudInformacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UadSolicitudInformacionRepository extends JpaRepository<UadSolicitudInformacion, UUID> {

    Optional<UadSolicitudInformacion> findFirstByNoticiaIdOrderByFechaSolicitudDesc(UUID noticiaId);
}

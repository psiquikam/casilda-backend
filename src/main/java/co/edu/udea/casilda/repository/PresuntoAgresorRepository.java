package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.PresuntoAgresor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PresuntoAgresorRepository extends JpaRepository<PresuntoAgresor, Long> {
    List<PresuntoAgresor> findByCasoId(Long casoId);
    void deleteByCasoId(Long casoId);

    /**
     * Busca la presunta persona agresora asociada a una solicitud
     */
    Optional<PresuntoAgresor> findBySolicitudAtencionId(Long solicitudId);

    /**
     * Elimina la presunta persona agresora asociada a una solicitud
     */
    void deleteBySolicitudAtencionId(Long solicitudId);
}

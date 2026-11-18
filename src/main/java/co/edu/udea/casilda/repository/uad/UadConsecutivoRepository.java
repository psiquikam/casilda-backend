package co.edu.udea.casilda.repository.uad;

import co.edu.udea.casilda.model.entity.uad.UadConsecutivo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UadConsecutivoRepository extends JpaRepository<UadConsecutivo, String> {

    /** Bloqueo pesimista (SELECT ... FOR UPDATE): dos radicaciones simultáneas no comparten número. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM UadConsecutivo c WHERE c.clave = :clave")
    Optional<UadConsecutivo> findForUpdate(@Param("clave") String clave);
}

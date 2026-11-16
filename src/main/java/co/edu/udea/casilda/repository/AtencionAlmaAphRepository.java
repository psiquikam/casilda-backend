package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtencionAlmaAphRepository extends JpaRepository<AtencionAlmaAph, Long> {
    /** Busca una atención por su identificador público. */
    Optional<AtencionAlmaAph> findByCodigo(String codigo);

    /** Bloquea una atención mientras se verifica y actualiza su versión. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AtencionAlmaAph a where a.codigo = :codigo")
    Optional<AtencionAlmaAph> findByCodigoForUpdate(@Param("codigo") String codigo);
}

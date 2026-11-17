package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.RutaExternaAlma;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RutaExternaAlmaRepository extends JpaRepository<RutaExternaAlma, Long> {
    /** Lista las rutas de una atención en orden de creación. */
    List<RutaExternaAlma> findByAtencionIdOrderByIdAsc(Long atencionId);

    /** Obtiene el identificador de la atención sin cargar la ruta antes de bloquearla. */
    @Query("select r.atencion.codigo from RutaExternaAlma r where r.id = :id")
    Optional<String> findCodigoAtencionById(@Param("id") Long id);

    /** Bloquea la ruta durante la comprobación de versión y actualización. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RutaExternaAlma r where r.id = :id")
    Optional<RutaExternaAlma> findByIdForUpdate(@Param("id") Long id);
}

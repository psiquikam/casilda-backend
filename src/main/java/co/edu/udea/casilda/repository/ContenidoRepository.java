package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.Contenido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar entidades Contenido (contenido destacado del Home).
 */
@Repository
public interface ContenidoRepository extends JpaRepository<Contenido, Long> {

    /**
     * Lista los contenidos vigentes al instante dado (no eliminados, con
     * vigenciaInicio ya alcanzada y vigenciaFin nula o aún no cumplida),
     * ordenados por orden ascendente y título ascendente como desempate.
     */
    @Query("""
            SELECT c FROM Contenido c
            WHERE c.eliminado = false
              AND c.vigenciaInicio <= :referencia
              AND (c.vigenciaFin IS NULL OR c.vigenciaFin >= :referencia)
            ORDER BY c.orden ASC, c.titulo ASC
            """)
    List<Contenido> findVigentes(@Param("referencia") Instant referencia);

    /**
     * Lista paginada de contenidos no eliminados lógicamente, para la tabla
     * de administración (incluye vigentes, futuros y expirados).
     */
    Page<Contenido> findByEliminadoFalseOrderByOrdenAscTituloAsc(Pageable pageable);

    /**
     * Busca un contenido administrable (no eliminado lógicamente) por su ID.
     */
    Optional<Contenido> findByIdAndEliminadoFalse(Long id);
}

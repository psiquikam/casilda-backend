package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.Cita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestionar entidades Cita
 */
@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    /**
     * Lista las citas de una solicitud de atención ordenadas por fecha descendente
     */
    List<Cita> findBySolicitudAtencionIdOrderByFechaDesc(Long solicitudId);

    @Query("""
            SELECT c FROM Cita c
            WHERE (:estadoId IS NULL OR c.estadoCita.id = :estadoId)
              AND (:estadoExcluidoId IS NULL OR c.estadoCita.id <> :estadoExcluidoId)
            ORDER BY CASE
                WHEN c.estadoCita.id IN :estadosPrioritarios THEN 0
                WHEN c.estadoCita.id = :estadoAtendida THEN 2
                ELSE 1
            END, c.fecha ASC, c.id ASC
            """)
    Page<Cita> findParaTabla(
            @Param("estadoId") Integer estadoId,
            @Param("estadoExcluidoId") Integer estadoExcluidoId,
            @Param("estadosPrioritarios") List<Integer> estadosPrioritarios,
            @Param("estadoAtendida") Integer estadoAtendida,
            Pageable pageable);

    @Query("""
            SELECT c FROM Cita c
            WHERE c.estadoCita.id IN :estadoIds
              AND NOT EXISTS (
                  SELECT caso.id FROM Caso caso
                  WHERE caso.cita.solicitudAtencion.id = c.solicitudAtencion.id
              )
            ORDER BY c.fecha DESC
            """)
    Page<Cita> findPendientesDeCaso(@Param("estadoIds") List<Integer> estadoIds, Pageable pageable);
}

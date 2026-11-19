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

    /**
     * Lista citas paginadas ordenadas por fecha descendente.
     */
    Page<Cita> findAllByOrderByFechaDesc(Pageable pageable);

    /**
     * Lista citas paginadas filtradas por estado.
     */
    Page<Cita> findByEstadoCitaIdOrderByFechaDesc(Integer estadoCitaId, Pageable pageable);

    /**
     * Lista citas paginadas excluyendo un estado.
     */
    Page<Cita> findByEstadoCitaIdNotOrderByFechaDesc(Integer estadoCitaId, Pageable pageable);

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

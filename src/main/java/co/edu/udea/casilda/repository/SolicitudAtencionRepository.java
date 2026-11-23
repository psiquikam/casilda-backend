package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.SolicitudAtencion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestionar entidades SolicitudAtencion
 */
@Repository
public interface SolicitudAtencionRepository extends JpaRepository<SolicitudAtencion, Long> {

	/**
	 * Lista solicitudes filtradas por estado de solicitud ordenadas por fecha de creación descendente.
	 */
	List<SolicitudAtencion> findByEstadoSolicitudIdOrderByFechaCreacionDesc(Integer estadoSolicitudId);

	/**
	 * Lista solicitudes paginadas ordenadas por fecha de creación descendente.
	 */
	Page<SolicitudAtencion> findAllByOrderByFechaCreacionDesc(Pageable pageable);

	@Query("""
			SELECT s FROM SolicitudAtencion s
			WHERE (:estadoId IS NULL OR s.estadoSolicitud.id = :estadoId)
			ORDER BY CASE
			    WHEN s.estadoSolicitud.id = :sinAsignarId AND s.asignaciones IS EMPTY THEN 0
			    ELSE 1
			END, CASE WHEN s.fechaCreacion IS NULL THEN 1 ELSE 0 END,
			s.fechaCreacion DESC, s.id DESC
			""")
	Page<SolicitudAtencion> findParaReparto(
			@Param("estadoId") Integer estadoId,
			@Param("sinAsignarId") Integer sinAsignarId,
			Pageable pageable);

	/**
	 * Lista solicitudes paginadas por estado ordenadas por fecha de creación descendente.
	 */
	Page<SolicitudAtencion> findByEstadoSolicitudIdOrderByFechaCreacionDesc(Integer estadoSolicitudId, Pageable pageable);
}

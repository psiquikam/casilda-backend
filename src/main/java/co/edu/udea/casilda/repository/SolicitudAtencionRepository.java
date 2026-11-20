package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.SolicitudAtencion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

	/**
	 * Lista solicitudes paginadas por estado ordenadas por fecha de creación descendente.
	 */
	Page<SolicitudAtencion> findByEstadoSolicitudIdOrderByFechaCreacionDesc(Integer estadoSolicitudId, Pageable pageable);

	/**
	 * Solicitudes radicadas por un usuario (D-UAD-16: quien no es del equipo de atención solo ve las suyas).
	 */
	List<SolicitudAtencion> findByUsuarioCreacionIdOrderByFechaCreacionDesc(Long usuarioId);

	List<SolicitudAtencion> findByUsuarioCreacionIdAndEstadoSolicitudIdOrderByFechaCreacionDesc(Long usuarioId, Integer estadoSolicitudId);

	Page<SolicitudAtencion> findByUsuarioCreacionIdOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

	Page<SolicitudAtencion> findByUsuarioCreacionIdAndEstadoSolicitudIdOrderByFechaCreacionDesc(Long usuarioId, Integer estadoSolicitudId, Pageable pageable);
}

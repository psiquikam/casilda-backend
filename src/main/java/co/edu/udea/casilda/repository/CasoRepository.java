package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar entidades Caso
 */
@Repository
public interface CasoRepository extends JpaRepository<Caso, Long> {
    
    /**
     * Busca un caso por su código único
     */
    Optional<Caso> findByCodigo(String codigo);

    /** Bloquea el caso para serializar los cambios de estado y fecha de cierre. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caso c where c.id = :id")
    Optional<Caso> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    /** Bloquea el caso identificado por código antes de validar su estado. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caso c where c.codigo = :codigo")
    Optional<Caso> findByCodigoForUpdate(@org.springframework.data.repository.query.Param("codigo") String codigo);

    /** Casos del Equipo de Atención de una persona por su documento (consulta de casos asociados UAD, D-UAD-18). */
    List<Caso> findByCitaSolicitudAtencionSolicitanteNumeroDocumento(String numeroDocumento);
    
    /**
     * Verifica si existe un caso con el código dado
     */
    boolean existsByCodigo(String codigo);
    
    /**
     * Cuenta los casos creados en un año específico
     * Usado para generar códigos secuenciales por año (ACO-2026-NNNN)
     */
    @Query("SELECT COUNT(c) FROM Caso c WHERE YEAR(c.fechaCreacion) = ?1")
    long countByYear(int year);

    /**
     * Retorna el máximo número consecutivo ya usado en los códigos ACO-YYYY-NNNN del año dado.
     * Evita colisiones cuando existen huecos en la secuencia por registros eliminados o rollbacks parciales.
     */
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo FROM 10) AS INTEGER)), 0) FROM caso WHERE codigo LIKE CONCAT('ACO-', CAST(:year AS VARCHAR), '-%')", nativeQuery = true)
    int findMaxSequentialNumberByYear(@org.springframework.data.repository.query.Param("year") int year);

    /**
     * Lista los casos asociados a una solicitud (a través de la cita).
     */
    List<Caso> findByCitaSolicitudAtencionIdOrderByFechaCreacionDesc(Long solicitudId);

    Optional<Caso> findFirstByCitaSolicitudAtencionIdOrderByFechaCreacionAsc(Long solicitudId);

    /**
     * Lista los casos asociados a una cita.
     */
    List<Caso> findByCitaIdOrderByFechaCreacionDesc(Long citaId);

    /**
     * Busca los códigos de casos que contengan el año especificado
     */
    @Query("SELECT c.codigo FROM Caso c WHERE c.codigo LIKE %:year%")
    List<String> findCodigosByYear(@org.springframework.data.repository.query.Param("year") String year);
}

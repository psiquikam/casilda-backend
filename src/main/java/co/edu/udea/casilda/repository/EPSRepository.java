package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.EPS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EPSRepository extends JpaRepository<EPS, Integer> {
    @Query(value = """
            SELECT e.*
            FROM eps e
            JOIN epsregimen er ON er.ideps = e.id
            WHERE er.idregimen = :regimenId
            ORDER BY e.nombre
            """, nativeQuery = true)
    List<EPS> findByRegimen(@Param("regimenId") Integer regimenId);
}

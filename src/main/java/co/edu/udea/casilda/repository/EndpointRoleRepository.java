package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.EndpointRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EndpointRoleRepository extends JpaRepository<EndpointRole, Long> {

    List<EndpointRole> findByEndpointId(final Long endpointId);
}

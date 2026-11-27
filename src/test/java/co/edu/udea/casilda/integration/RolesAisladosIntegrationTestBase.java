package co.edu.udea.casilda.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

abstract class RolesAisladosIntegrationTestBase extends IntegrationTestBase {
    private static final PostgreSQLContainer<?> POSTGRES_ROLES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("casilda")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES_ROLES.start();
    }

    @DynamicPropertySource
    static void registerRolesDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_ROLES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_ROLES::getUsername);
        registry.add("spring.datasource.password", POSTGRES_ROLES::getPassword);
    }
}

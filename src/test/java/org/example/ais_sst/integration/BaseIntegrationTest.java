package org.example.ais_sst.integration;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.api.DBRider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine")
    )
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Важно: параметр stringtype=unspecified должен быть в URL
        String jdbcUrl = String.format(
                "jdbc:postgresql://localhost:%d/testdb?stringtype=unspecified",
                postgres.getMappedPort(5432)
        );

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.liquibase.drop-first", () -> true);
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

        @Autowired
    protected DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        if (databaseCleaner != null) {
            databaseCleaner.clearDatabase();
        }
    }

    static {
        // Принудительно указываем путь к Docker сокету
        System.setProperty("DOCKER_HOST", "unix:///var/run/docker.sock");
        // Отключаем проверку версии
        System.setProperty("docker.client.version.verify", "false");
    }
}

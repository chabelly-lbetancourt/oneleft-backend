package es.upm.miw.oneleft.plans;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Infraestructura real para los tests de integración: PostgreSQL con PostGIS y RabbitMQ en contenedores.
 * Spring Boot configura la conexión automáticamente con @ServiceConnection.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgis() {
        // Imagen multiarquitectura (amd64 y arm64) de PostGIS, compatible con la de PostgreSQL
        return new PostgreSQLContainer(DockerImageName.parse("imresamu/postgis:17-3.5")
                .asCompatibleSubstituteFor("postgres"));
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitmq() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management-alpine"));
    }
}

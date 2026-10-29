package es.upm.miw.oneleft.users;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * RabbitMQ in a container, for the tests that start the whole application: the service announces the deletion of
 * accounts on it (HU-045), and its health depends on the broker. The other tests simulate the announcement.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RabbitContainerConfiguration {

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitmq() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management-alpine"));
    }
}

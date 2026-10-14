package es.upm.miw.oneleft.users;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * The S3-compatible storage of the photos in a container (Versity S3 Gateway, as in Docker Compose), for the tests
 * that need it. Its metadata go to a sidecar folder, so it works on any file system.
 */
@TestConfiguration(proxyBeanMethods = false)
public class StorageContainerConfiguration {

    private static final int PORT = 7070;
    private static final String ACCESS_KEY = "test-access";
    private static final String SECRET_KEY = "test-secret-key";

    @Bean
    GenericContainer<?> storage() {
        return new GenericContainer<>(DockerImageName.parse("versity/versitygw:v1.8.0"))
                .withEnv("ROOT_ACCESS_KEY", ACCESS_KEY)
                .withEnv("ROOT_SECRET_KEY", SECRET_KEY)
                .withCreateContainerCmdModifier(command -> command.withEntrypoint("sh"))
                .withCommand("-c", "mkdir -p /s3/data /s3/meta && exec versitygw posix --sidecar /s3/meta /s3/data")
                .withExposedPorts(PORT)
                .waitingFor(Wait.forListeningPort());
    }

    /** The tests reach the storage at the same address as the service, so the signed links work for both */
    @Bean
    DynamicPropertyRegistrar storageProperties(GenericContainer<?> storage) {
        return registry -> {
            var endpoint = "http://" + storage.getHost() + ":" + storage.getMappedPort(PORT);
            registry.add("oneleft.storage.endpoint", () -> endpoint);
            registry.add("oneleft.storage.public-endpoint", () -> endpoint);
            registry.add("oneleft.storage.access-key", () -> ACCESS_KEY);
            registry.add("oneleft.storage.secret-key", () -> SECRET_KEY);
            registry.add("oneleft.storage.create-bucket", () -> "true");
        };
    }
}

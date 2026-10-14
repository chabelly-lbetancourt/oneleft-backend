package es.upm.miw.oneleft.users.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * The S3-compatible storage of the photos: Versity S3 Gateway in Docker, S3 or a Lightsail bucket when deployed. The
 * keys come from the environment ({@code STORAGE_ACCESS_KEY}, {@code STORAGE_SECRET_KEY}).
 *
 * @param endpoint       where this service reaches the storage
 * @param publicEndpoint where browsers reach it: the signed links are signed for this address
 * @param pathStyle      {@code bucket} in the path instead of in the host name (S3-compatible servers)
 * @param createBucket   creates the bucket on startup if it does not exist (development)
 */
@ConfigurationProperties("oneleft.storage")
public record StorageProperties(URI endpoint, URI publicEndpoint, String bucket, String region, String accessKey,
                                String secretKey, boolean pathStyle, boolean createBucket) {
}

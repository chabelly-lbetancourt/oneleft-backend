package es.upm.miw.oneleft.users.domain.model;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * A signed link to upload a photo straight to the storage, so that the photo never goes through the backend. It only
 * accepts a {@code PUT} with the declared size and type, until it expires.
 *
 * @param key       where the photo will be kept, to confirm it afterwards
 * @param headers   the headers the upload must send, exactly
 * @param expiresAt when the link stops working
 */
public record PhotoUpload(String key, URI url, Map<String, String> headers, Instant expiresAt) {

    public PhotoUpload {
        headers = Map.copyOf(headers);
    }
}

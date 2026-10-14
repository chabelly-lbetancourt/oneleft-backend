package es.upm.miw.oneleft.users.domain.port.in;

import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.PhotoUpload;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Profile photo (HU-020): the photo goes straight from the device to the storage with a signed link, and the backend
 * only keeps where it is. Photos are seen through signed links that expire, and only with a session.
 */
public interface ManagePhotoUseCase {

    int MAX_PEOPLE = 50;

    /**
     * A signed link to upload my new photo.
     *
     * @throws es.upm.miw.oneleft.users.domain.model.ValidationException if the photo is not WebP or JPEG or too big
     */
    PhotoUpload startUpload(Identity identity, String contentType, long size);

    /**
     * Makes the uploaded photo mine, replacing the previous one.
     *
     * @return a signed link to see it
     * @throws es.upm.miw.oneleft.users.domain.model.ValidationException if the key is not mine or nothing valid was
     *                                                                   uploaded there
     */
    URI confirmUpload(Identity identity, String key);

    void removeMyPhoto(Identity identity);

    /**
     * Signed links to the photos of up to {@link #MAX_PEOPLE} people; whoever has none is left out.
     */
    Map<UUID, URI> photosOf(Collection<UUID> userIds);
}

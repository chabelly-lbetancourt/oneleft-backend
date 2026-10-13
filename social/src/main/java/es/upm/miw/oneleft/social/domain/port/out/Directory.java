package es.upm.miw.oneleft.social.domain.port.out;

import java.util.UUID;

/** The accounts of OneLeft: the name of someone the social service does not know yet. */
public interface Directory {

    /**
     * The display name of {@code userId}.
     *
     * @throws es.upm.miw.oneleft.social.domain.model.SocialException {@code PERSON_NOT_FOUND} if nobody has that id,
     *                                                                  {@code UNAVAILABLE} if it cannot be asked
     */
    String nameOf(UUID userId);
}

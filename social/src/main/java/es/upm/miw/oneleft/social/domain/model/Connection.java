package es.upm.miw.oneleft.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Someone in one of my lists (followers, following or requests) and since when. */
public record Connection(UUID userId, String name, FollowStatus status, Instant since) {

    public static Connection of(Person person, Follow follow) {
        return new Connection(person.userId(), person.name(), follow.status(), follow.since());
    }
}

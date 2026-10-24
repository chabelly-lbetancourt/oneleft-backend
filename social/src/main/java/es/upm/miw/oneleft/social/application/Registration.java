package es.upm.miw.oneleft.social.application;

import es.upm.miw.oneleft.social.domain.model.Caller;
import es.upm.miw.oneleft.social.domain.model.Person;
import es.upm.miw.oneleft.social.domain.port.out.Directory;
import es.upm.miw.oneleft.social.domain.port.out.People;

import java.util.UUID;

/**
 * Everyone the service deals with, as a person of the service: whoever uses it is registered with the name of their
 * account; someone new is looked up once in the users service. A profile is private until its owner opens it.
 */
final class Registration {

    private final People people;
    private final Directory directory;

    Registration(People people, Directory directory) {
        this.people = people;
        this.directory = directory;
    }

    /** The caller as a person of the service: created the first time, with the current name of their account. */
    Person register(Caller me) {
        var known = people.find(me.userId());
        if (known.isEmpty()) {
            return people.save(new Person(me.userId(), me.name(), SocialService.DEFAULT_VISIBILITY,
                    SocialService.DEFAULT_FEED_AUDIENCE));
        }
        var renamed = known.get().renamed(me.name());
        return renamed == known.get() ? renamed : people.save(renamed);
    }

    /** Someone else, kept as a person of the service the first time (their name comes from the users service). */
    Person known(UUID userId) {
        return people.find(userId).orElseGet(() -> people.save(stranger(userId)));
    }

    /** Someone the service does not know yet: their name from the users service, private until they choose. */
    Person stranger(UUID userId) {
        return new Person(userId, directory.nameOf(userId), SocialService.DEFAULT_VISIBILITY,
                SocialService.DEFAULT_FEED_AUDIENCE);
    }
}

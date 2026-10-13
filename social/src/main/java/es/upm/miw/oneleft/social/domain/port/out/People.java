package es.upm.miw.oneleft.social.domain.port.out;

import es.upm.miw.oneleft.social.domain.model.Person;

import java.util.Optional;
import java.util.UUID;

/** The people known by the social service. */
public interface People {

    Optional<Person> find(UUID userId);

    Person save(Person person);
}

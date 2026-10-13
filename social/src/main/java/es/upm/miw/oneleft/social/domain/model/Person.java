package es.upm.miw.oneleft.social.domain.model;

import java.util.UUID;

/**
 * Someone in the social service: the name shown in the lists of followers and who may follow them. The name is a copy
 * of the one of the account, refreshed whenever the person uses the service.
 */
public record Person(UUID userId, String name, Visibility visibility) {

    public static final int MAX_NAME_LENGTH = 50;

    public Person {
        if (userId == null || visibility == null) {
            throw new ValidationException("person.missingData", "Required person data is missing");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("person.name", "A person needs a name");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH).strip();
        }
    }

    /** The same person with the current name of their account; itself when it has not changed. */
    public Person renamed(String newName) {
        var renamed = new Person(userId, newName, visibility);
        return renamed.equals(this) ? this : renamed;
    }

    public Person withVisibility(Visibility newVisibility) {
        return new Person(userId, name, newVisibility);
    }
}

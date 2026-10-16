package es.upm.miw.oneleft.social.domain.model;

import java.util.UUID;

/**
 * Someone in the social service: the name shown in the lists of followers, who may follow them and who sees their
 * activity in the feeds (HU-059). The name is a copy of the one of the account, refreshed whenever the person uses the
 * service.
 */
public record Person(UUID userId, String name, Visibility visibility, FeedAudience feedAudience) {

    public static final int MAX_NAME_LENGTH = 50;

    public Person {
        if (userId == null || visibility == null || feedAudience == null) {
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
        var renamed = new Person(userId, newName, visibility, feedAudience);
        return renamed.equals(this) ? this : renamed;
    }

    public Person withVisibility(Visibility newVisibility) {
        return new Person(userId, name, newVisibility, feedAudience);
    }

    public Person withFeedAudience(FeedAudience newAudience) {
        return new Person(userId, name, visibility, newAudience);
    }

    /** Whether their followers see what they do in the plans (HU-059). */
    public boolean sharesActivity() {
        return feedAudience == FeedAudience.FOLLOWERS;
    }
}

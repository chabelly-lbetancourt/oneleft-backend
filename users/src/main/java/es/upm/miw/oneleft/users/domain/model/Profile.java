package es.upm.miw.oneleft.users.domain.model;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Public profile of a user: display name, approximate zone and hobbies with their level.
 */
public record Profile(UUID userId, String displayName, ApproximateZone zone, List<Hobby> hobbies) {

    public static final int MAX_DISPLAY_NAME_LENGTH = 50;
    public static final int MAX_HOBBIES = 10;

    public Profile {
        if (userId == null) {
            throw new ValidationException("profile.userRequired", "The profile must belong to a user");
        }
        if (displayName == null || displayName.isBlank() || displayName.strip().length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new ValidationException("profile.displayName",
                    "The display name is required and allows up to " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
        displayName = displayName.strip();
        hobbies = hobbies == null ? List.of() : List.copyOf(hobbies);
        if (hobbies.size() > MAX_HOBBIES) {
            throw new ValidationException("profile.tooManyHobbies", "At most " + MAX_HOBBIES + " hobbies are allowed");
        }
        if (hobbies.stream().map(Hobby::activity).distinct().count() != hobbies.size()) {
            throw new ValidationException("profile.duplicateActivity", "Each activity can only appear once");
        }
    }

    /** Initial profile of a newcomer: their Keycloak name, with no zone or hobbies. */
    public static Profile initial(User user) {
        return new Profile(user.id(), user.name(), null, List.of());
    }

    public Profile update(String newDisplayName, ApproximateZone newZone, Collection<Hobby> newHobbies) {
        return new Profile(userId, newDisplayName, newZone, newHobbies == null ? null : List.copyOf(newHobbies));
    }
}

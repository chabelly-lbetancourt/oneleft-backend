package es.upm.miw.oneleft.notifications.domain.model;

import java.util.Map;
import java.util.UUID;

/**
 * For whom a plan is (HU-070): everyone (the default), only women or only men, and an optional minimum and maximum
 * age. Who is unknown here is treated as the search of the plans service does: they only hear about plans for everyone.
 */
public record Audience(AudienceGender gender, Integer minAge, Integer maxAge) {

    public static final Audience EVERYONE = new Audience(AudienceGender.ALL, null, null);

    public Audience {
        gender = gender == null ? AudienceGender.ALL : gender;
    }

    /** Everyone and no ages: nothing to check. */
    public boolean isEveryone() {
        return gender == AudienceGender.ALL && minAge == null && maxAge == null;
    }

    /** Whether the plan is for that person; {@code null} is a person whose data is not known. */
    public boolean fits(MemberAudience member) {
        if (isEveryone()) {
            return true;
        }
        return member != null && gender.fits(member.gender()) && (minAge == null || member.age() >= minAge)
                && (maxAge == null || member.age() <= maxAge);
    }

    public boolean fits(UUID userId, Map<UUID, MemberAudience> known) {
        return fits(known.get(userId));
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import java.util.Map;

/**
 * What the plans service knows about a person to decide whether a plan is for them (HU-070): gender, age, the age band
 * other people see instead of it (it can be {@code null}) and their level in each activity of their profile; never the
 * date of birth.
 */
public record AudienceMember(PersonGender gender, int age, String ageBand, Map<Activity, Level> levels) {

    public AudienceMember {
        levels = levels == null ? Map.of() : Map.copyOf(levels);
    }

    public AudienceMember(PersonGender gender, int age) {
        this(gender, age, null, null);
    }

    /** Their level in the activity, or {@code null} if it is not in their profile. */
    public Level levelIn(Activity activity) {
        return levels.get(activity);
    }
}

package es.upm.miw.oneleft.plans.domain.model;

/**
 * For whom a plan is (HU-070): everyone (the default), only women or only men, and an optional minimum and maximum age.
 * People who are «non-binary» or «prefer not to say» fit the plans for everyone only.
 */
public record Audience(AudienceGender gender, Integer minAge, Integer maxAge) {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 120;
    public static final Audience EVERYONE = new Audience(AudienceGender.ALL, null, null);

    public Audience {
        gender = gender == null ? AudienceGender.ALL : gender;
        if (minAge != null && (minAge < MIN_AGE || minAge > MAX_AGE)
                || maxAge != null && (maxAge < MIN_AGE || maxAge > MAX_AGE)) {
            throw new ValidationException("plan.audienceAge", "Ages go from " + MIN_AGE + " to " + MAX_AGE);
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new ValidationException("plan.audienceAge", "The minimum age cannot be above the maximum");
        }
    }

    /** Everyone and no ages: nothing to check. */
    public boolean isEveryone() {
        return gender == AudienceGender.ALL && minAge == null && maxAge == null;
    }

    public boolean fits(AudienceMember member) {
        return gender.fits(member.gender()) && (minAge == null || member.age() >= minAge)
                && (maxAge == null || member.age() <= maxAge);
    }
}

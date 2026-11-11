package es.upm.miw.oneleft.plans.domain.model;

/**
 * What the plans service knows about a person to decide whether a plan is for them (HU-070): gender, age and the age band
 * other people see instead of it (it can be {@code null}); never the date of birth.
 */
public record AudienceMember(PersonGender gender, int age, String ageBand) {

    public AudienceMember(PersonGender gender, int age) {
        this(gender, age, null);
    }
}

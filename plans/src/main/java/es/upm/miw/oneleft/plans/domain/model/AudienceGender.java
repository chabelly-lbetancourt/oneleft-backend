package es.upm.miw.oneleft.plans.domain.model;

/** For whom a plan is, by gender (HU-070): everyone, only women or only men. */
public enum AudienceGender {
    ALL, WOMAN, MAN;

    boolean fits(PersonGender gender) {
        return this == ALL || this.name().equals(gender.name());
    }
}

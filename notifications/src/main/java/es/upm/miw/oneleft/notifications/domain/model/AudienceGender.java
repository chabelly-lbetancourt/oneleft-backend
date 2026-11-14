package es.upm.miw.oneleft.notifications.domain.model;

/** For whom a plan is, by gender (HU-070): everyone, only women or only men. */
public enum AudienceGender {
    ALL, WOMAN, MAN;

    /** Non-binary people and who prefers not to say fit only the plans for everyone. */
    boolean fits(String memberGender) {
        return this == ALL || this.name().equals(memberGender);
    }
}

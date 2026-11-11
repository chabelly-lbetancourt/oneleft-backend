package es.upm.miw.oneleft.plans.domain.model;

/** What the plans service knows about a person to decide whether a plan is for them (HU-070): gender and age, never the date of birth. */
public record AudienceMember(PersonGender gender, int age) {
}

package es.upm.miw.oneleft.reputation.domain.model;

/** How many people vouch for a level of someone in an activity (HU-061): «Intermediate in padel (endorsed by 7)». */
public record EndorsementCount(String activity, SkillLevel level, long count) {
}

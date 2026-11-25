package es.upm.miw.oneleft.plans.domain.model;

/**
 * Why a plan fits a person more or less (HU-031). Every compatibility carries one reason about the activity and one
 * about the level, which the app shows in the language of the person.
 */
public enum CompatibilityReason {
    /** The activity of the plan is one of the hobbies of their profile. */
    SAME_ACTIVITY,
    /** It is not one of their hobbies, but one of them is of the same category (sport, culture...). */
    SIMILAR_ACTIVITY,
    /** Nothing in their profile is like it. */
    NEW_ACTIVITY,
    /** The plan is of the level they have in that hobby. */
    SAME_LEVEL,
    /** The plan is one level above or below theirs. */
    CLOSE_LEVEL,
    /** The plan is two levels away from theirs. */
    LEVEL_GAP,
    /** The plan is for any level. */
    ANY_LEVEL,
    /** They do not have the activity in their profile and the plan is for beginners. */
    BEGINNER_FRIENDLY,
    /** They do not have the activity in their profile and the plan asks for some level. */
    NEEDS_EXPERIENCE
}

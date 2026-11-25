package es.upm.miw.oneleft.plans.domain.model;

import java.util.List;
import java.util.Map;

/**
 * How well a plan fits a person (HU-031): a score from 0 to 100 and the reasons behind it. It is a fixed set of
 * rules, not a model: the same plan and the same profile always give the same result, and each point can be
 * explained.
 * <p>
 * It only uses what is already known: the activity and the level of the plan, and the hobbies of the profile with
 * the level of the person in each. Two parts are added up:
 * <ul>
 *   <li><b>Activity</b>, up to {@value #ACTIVITY_POINTS}: all of them if it is one of their hobbies, half if one of
 *   their hobbies is of the same category, none otherwise. {@link Activity#OTHER} is like nothing else.</li>
 *   <li><b>Level</b>, up to {@value #LEVEL_POINTS}: all of them if the plan is for any level or for theirs, half if
 *   it is one level away, none if it is two. In an activity that is not in their profile they count as a
 *   beginner.</li>
 * </ul>
 *
 * @param score   from 0 (nothing in common) to 100 (a hobby of theirs, at their level)
 * @param reasons the reason about the activity and then the one about the level
 */
public record Compatibility(int score, List<CompatibilityReason> reasons) {

    public static final int ACTIVITY_POINTS = 60;
    public static final int LEVEL_POINTS = 40;
    public static final int MAX_SCORE = ACTIVITY_POINTS + LEVEL_POINTS;

    public Compatibility {
        if (score < 0 || score > MAX_SCORE) {
            throw new IllegalArgumentException("A compatibility goes from 0 to " + MAX_SCORE);
        }
        reasons = List.copyOf(reasons);
    }

    /** The compatibility of a plan with a person, from the hobbies of their profile. */
    public static Compatibility of(Plan plan, AudienceMember person) {
        return of(plan.activity(), plan.level(), person.levels());
    }

    /**
     * @param planLevel the level of the plan, or {@code null} if it is for any level
     * @param hobbies   the hobbies of the person with their level in each; empty if they have none
     */
    public static Compatibility of(Activity activity, Level planLevel, Map<Activity, Level> hobbies) {
        // A hobby always has a level: without one, the activity is not in their profile
        var ownLevel = hobbies.get(activity);
        var activityReason = ownLevel != null ? CompatibilityReason.SAME_ACTIVITY
                : sharesCategory(activity, hobbies) ? CompatibilityReason.SIMILAR_ACTIVITY
                : CompatibilityReason.NEW_ACTIVITY;
        var levelReason = levelReason(planLevel, ownLevel);
        return new Compatibility(activityPoints(activityReason) + levelPoints(planLevel, ownLevel),
                List.of(activityReason, levelReason));
    }

    private static boolean sharesCategory(Activity activity, Map<Activity, Level> hobbies) {
        var category = activity.category();
        return category != ActivityCategory.OTHER
                && hobbies.keySet().stream().anyMatch(hobby -> hobby.category() == category);
    }

    private static int activityPoints(CompatibilityReason reason) {
        return switch (reason) {
            case SAME_ACTIVITY -> ACTIVITY_POINTS;
            case SIMILAR_ACTIVITY -> ACTIVITY_POINTS / 2;
            default -> 0;
        };
    }

    /** Levels apart: 0, 1 or 2. Without a level of their own in the activity, the person counts as a beginner. */
    private static int gap(Level planLevel, Level ownLevel) {
        var own = ownLevel == null ? Level.BEGINNER : ownLevel;
        return Math.abs(planLevel.ordinal() - own.ordinal());
    }

    private static int levelPoints(Level planLevel, Level ownLevel) {
        if (planLevel == null) {
            return LEVEL_POINTS;
        }
        return switch (gap(planLevel, ownLevel)) {
            case 0 -> LEVEL_POINTS;
            case 1 -> LEVEL_POINTS / 2;
            default -> 0;
        };
    }

    private static CompatibilityReason levelReason(Level planLevel, Level ownLevel) {
        if (planLevel == null) {
            return CompatibilityReason.ANY_LEVEL;
        }
        var gap = gap(planLevel, ownLevel);
        if (ownLevel == null) {
            return gap == 0 ? CompatibilityReason.BEGINNER_FRIENDLY : CompatibilityReason.NEEDS_EXPERIENCE;
        }
        return switch (gap) {
            case 0 -> CompatibilityReason.SAME_LEVEL;
            case 1 -> CompatibilityReason.CLOSE_LEVEL;
            default -> CompatibilityReason.LEVEL_GAP;
        };
    }
}

package es.upm.miw.oneleft.plans.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** The groups of activities of the selector and of the filter of nearby plans (HU-068). */
public enum ActivityCategory {
    SPORT, CULTURE, LEISURE, FOOD_DRINK, OTHER;

    /** The activities of this category. */
    public List<Activity> activities() {
        return Arrays.stream(Activity.values()).filter(activity -> activity.category() == this).toList();
    }

    /** The activities of all these categories together. */
    public static Set<Activity> activitiesOf(Iterable<ActivityCategory> categories) {
        var all = new java.util.LinkedHashSet<Activity>();
        categories.forEach(category -> all.addAll(category.activities()));
        return all.stream().collect(Collectors.toUnmodifiableSet());
    }
}

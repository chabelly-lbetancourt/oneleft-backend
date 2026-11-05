package es.upm.miw.oneleft.plans.domain.model;

/**
 * Plan activities (HU-068), grouped by category. Same catalog as the users and notifications services; each service
 * keeps its own copy so that no code is shared between bounded contexts (the codes are the contract). This is the one
 * place that says which activities are {@code outdoor} (the weather matters, HU-026) and which category each one is in.
 * {@link #OTHER} is for anything else: the plan carries a short text of its own.
 */
public enum Activity {
    PADEL(ActivityCategory.SPORT, true), FOOTBALL(ActivityCategory.SPORT, true),
    BASKETBALL(ActivityCategory.SPORT, true), TENNIS(ActivityCategory.SPORT, true),
    RUNNING(ActivityCategory.SPORT, true), CYCLING(ActivityCategory.SPORT, true),
    HIKING(ActivityCategory.SPORT, true), VOLLEYBALL(ActivityCategory.SPORT, true),
    SWIMMING(ActivityCategory.SPORT, false), CLIMBING(ActivityCategory.SPORT, true),
    GYM(ActivityCategory.SPORT, false), YOGA(ActivityCategory.SPORT, false),
    SKATING(ActivityCategory.SPORT, true), PADEL_SURF(ActivityCategory.SPORT, true),
    PING_PONG(ActivityCategory.SPORT, false), PETANQUE(ActivityCategory.SPORT, true),

    CINEMA(ActivityCategory.CULTURE, false), CONCERTS(ActivityCategory.CULTURE, false),
    THEATER(ActivityCategory.CULTURE, false), MUSEUMS_EXHIBITIONS(ActivityCategory.CULTURE, false),

    BOARD_GAMES(ActivityCategory.LEISURE, false), VIDEO_GAMES(ActivityCategory.LEISURE, false),
    DANCING(ActivityCategory.LEISURE, false), LANGUAGE_EXCHANGE(ActivityCategory.LEISURE, false),
    ESCAPE_ROOM(ActivityCategory.LEISURE, false),

    TAPAS(ActivityCategory.FOOD_DRINK, false), MEAL(ActivityCategory.FOOD_DRINK, false),
    COFFEE(ActivityCategory.FOOD_DRINK, false),

    OTHER(ActivityCategory.OTHER, false);

    private final ActivityCategory category;
    private final boolean outdoor;

    Activity(ActivityCategory category, boolean outdoor) {
        this.category = category;
        this.outdoor = outdoor;
    }

    public ActivityCategory category() {
        return category;
    }

    /** Usually played outdoors: its plans show the weather forecast (HU-026). */
    public boolean outdoor() {
        return outdoor;
    }
}

package es.upm.miw.oneleft.plans.domain.model;

/**
 * Plan activities. Same catalog as the users service; each service keeps its own copy so that no code is shared
 * between bounded contexts (the codes are the contract). {@code outdoor} says whether the weather matters (HU-026).
 */
public enum Activity {
    PADEL(true), FOOTBALL(true), BASKETBALL(true), TENNIS(true), RUNNING(true), CYCLING(true), HIKING(true),
    BOARD_GAMES(false), CINEMA(false), CONCERTS(false);

    private final boolean outdoor;

    Activity(boolean outdoor) {
        this.outdoor = outdoor;
    }

    /** Usually played outdoors: its plans show the weather forecast (HU-026). */
    public boolean outdoor() {
        return outdoor;
    }
}

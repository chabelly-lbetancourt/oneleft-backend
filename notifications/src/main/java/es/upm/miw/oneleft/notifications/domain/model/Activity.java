package es.upm.miw.oneleft.notifications.domain.model;

/**
 * Plan activities (HU-068). Same catalog as the users and plans services; each service keeps its own copy so that no
 * code is shared between bounded contexts (the codes are the contract). {@link #OTHER} is here to read the plans of
 * another activity, but nobody can ask to hear about it: see {@link #isOfInterest()}.
 */
public enum Activity {
    // Sport
    PADEL, FOOTBALL, BASKETBALL, TENNIS, RUNNING, CYCLING, HIKING, VOLLEYBALL, SWIMMING, CLIMBING, GYM, YOGA,
    SKATING, PADEL_SURF, PING_PONG, PETANQUE,
    // Culture
    CINEMA, CONCERTS, THEATER, MUSEUMS_EXHIBITIONS,
    // Leisure
    BOARD_GAMES, VIDEO_GAMES, DANCING, LANGUAGE_EXCHANGE, ESCAPE_ROOM,
    // Food and drink
    TAPAS, MEAL, COFFEE,
    OTHER;

    /** Whether a person can choose it as an interest of their notices or alerts: «other» is not an interest. */
    public boolean isOfInterest() {
        return this != OTHER;
    }
}

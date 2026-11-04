package es.upm.miw.oneleft.users.domain.model;

/**
 * Activities that plans can be published for (HU-068), the ones a person can have as a hobby. Same catalog as the plans
 * service without {@code OTHER}, which only makes sense for a plan. Clients translate the codes into the user's
 * language.
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
    TAPAS, MEAL, COFFEE
}

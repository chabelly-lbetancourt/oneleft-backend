package es.upm.miw.oneleft.notifications.domain.model;

/**
 * Plan activities. Same catalog as the users and plans services; each service keeps its own copy so that no code is shared
 * between bounded contexts (the codes are the contract).
 */
public enum Activity {
    PADEL, FOOTBALL, BASKETBALL, TENNIS, RUNNING, CYCLING, HIKING, BOARD_GAMES, CINEMA, CONCERTS
}

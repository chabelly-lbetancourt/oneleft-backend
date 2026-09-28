package es.upm.miw.oneleft.users.domain.model;

/**
 * Actividades para las que se pueden publicar planes.
 */
public enum Activity {
    PADEL("Pádel"),
    FUTBOL("Fútbol"),
    BALONCESTO("Baloncesto"),
    TENIS("Tenis"),
    RUNNING("Running"),
    CICLISMO("Ciclismo"),
    SENDERISMO("Senderismo"),
    JUEGOS_DE_MESA("Juegos de mesa"),
    CINE("Cine"),
    CONCIERTOS("Conciertos");

    private final String displayName;

    Activity(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.PlanCancellation;
import es.upm.miw.oneleft.notifications.domain.model.PlanReminder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Text of a system notification in the language of the browser's subscription. Inside the app the web writes the
 * notice itself with its translation files; here the server has to, because the notification is shown with the app
 * closed.
 */
final class NoticeTexts {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    /** Names of every activity (HU-068); «other» is only «Other activity»: the plan page has its own text. */
    private static final Map<Activity, String> SPANISH = Map.ofEntries(
            Map.entry(Activity.PADEL, "Pádel"),
            Map.entry(Activity.FOOTBALL, "Fútbol"),
            Map.entry(Activity.BASKETBALL, "Baloncesto"),
            Map.entry(Activity.TENNIS, "Tenis"),
            Map.entry(Activity.RUNNING, "Running"),
            Map.entry(Activity.CYCLING, "Ciclismo"),
            Map.entry(Activity.HIKING, "Senderismo"),
            Map.entry(Activity.VOLLEYBALL, "Vóley"),
            Map.entry(Activity.SWIMMING, "Natación"),
            Map.entry(Activity.CLIMBING, "Escalada"),
            Map.entry(Activity.GYM, "Gimnasio"),
            Map.entry(Activity.YOGA, "Yoga"),
            Map.entry(Activity.SKATING, "Patinaje"),
            Map.entry(Activity.PADEL_SURF, "Pádel surf"),
            Map.entry(Activity.PING_PONG, "Pin-pon"),
            Map.entry(Activity.PETANQUE, "Petanca"),
            Map.entry(Activity.CINEMA, "Cine"),
            Map.entry(Activity.CONCERTS, "Conciertos"),
            Map.entry(Activity.THEATER, "Teatro"),
            Map.entry(Activity.MUSEUMS_EXHIBITIONS, "Museos y exposiciones"),
            Map.entry(Activity.BOARD_GAMES, "Juegos de mesa"),
            Map.entry(Activity.VIDEO_GAMES, "Videojuegos"),
            Map.entry(Activity.DANCING, "Baile"),
            Map.entry(Activity.LANGUAGE_EXCHANGE, "Intercambio de idiomas"),
            Map.entry(Activity.ESCAPE_ROOM, "Escape room"),
            Map.entry(Activity.TAPAS, "Tapas"),
            Map.entry(Activity.MEAL, "Comida o cena"),
            Map.entry(Activity.COFFEE, "Café"),
            Map.entry(Activity.OTHER, "Otra actividad"));
    private static final Map<Activity, String> ENGLISH = Map.ofEntries(
            Map.entry(Activity.PADEL, "Padel"),
            Map.entry(Activity.FOOTBALL, "Football"),
            Map.entry(Activity.BASKETBALL, "Basketball"),
            Map.entry(Activity.TENNIS, "Tennis"),
            Map.entry(Activity.RUNNING, "Running"),
            Map.entry(Activity.CYCLING, "Cycling"),
            Map.entry(Activity.HIKING, "Hiking"),
            Map.entry(Activity.VOLLEYBALL, "Volleyball"),
            Map.entry(Activity.SWIMMING, "Swimming"),
            Map.entry(Activity.CLIMBING, "Climbing"),
            Map.entry(Activity.GYM, "Gym"),
            Map.entry(Activity.YOGA, "Yoga"),
            Map.entry(Activity.SKATING, "Skating"),
            Map.entry(Activity.PADEL_SURF, "Padel surf"),
            Map.entry(Activity.PING_PONG, "Ping-pong"),
            Map.entry(Activity.PETANQUE, "Petanque"),
            Map.entry(Activity.CINEMA, "Cinema"),
            Map.entry(Activity.CONCERTS, "Concerts"),
            Map.entry(Activity.THEATER, "Theatre"),
            Map.entry(Activity.MUSEUMS_EXHIBITIONS, "Museums and exhibitions"),
            Map.entry(Activity.BOARD_GAMES, "Board games"),
            Map.entry(Activity.VIDEO_GAMES, "Video games"),
            Map.entry(Activity.DANCING, "Dancing"),
            Map.entry(Activity.LANGUAGE_EXCHANGE, "Language exchange"),
            Map.entry(Activity.ESCAPE_ROOM, "Escape room"),
            Map.entry(Activity.TAPAS, "Tapas"),
            Map.entry(Activity.MEAL, "Meal"),
            Map.entry(Activity.COFFEE, "Coffee"),
            Map.entry(Activity.OTHER, "Other activity"));

    private NoticeTexts() {
    }

    static String title(NearbyPlanNotice notice, String language) {
        return (english(language) ? "Plan nearby: " : "Plan cerca: ") + notice.title();
    }

    static String body(NearbyPlanNotice notice, String language, ZoneId zone) {
        var time = TIME.format(notice.startsAt().atZone(zone));
        var km = String.format(english(language) ? java.util.Locale.ENGLISH : java.util.Locale.forLanguageTag("es"),
                "%.1f", notice.distanceMeters() / 1000.0);
        var spots = notice.freeSpots();
        return english(language)
                ? "%s at %s · %s · %s km away · %d %s left".formatted(ENGLISH.get(notice.activity()), time,
                notice.placeName(), km, spots, spots == 1 ? "spot" : "spots")
                : "%s a las %s · %s · a %s km · %s %d".formatted(SPANISH.get(notice.activity()), time,
                notice.placeName(), km, spots == 1 ? "Falta" : "Faltan", spots);
    }

    static String title(PlanReminder reminder, String language) {
        return (english(language) ? "Starting soon: " : "Empieza pronto: ") + reminder.title();
    }

    static String body(PlanReminder reminder, String language, ZoneId zone) {
        var time = TIME.format(reminder.startsAt().atZone(zone));
        return english(language)
                ? "At %s · %s. Tap to see who is going.".formatted(time, reminder.placeName())
                : "A las %s · %s. Toca para ver quién va.".formatted(time, reminder.placeName());
    }

    static String title(PlanCancellation cancellation, String language) {
        return (english(language) ? "Plan cancelled: " : "Plan cancelado: ") + cancellation.title();
    }

    static String body(PlanCancellation cancellation, String language, ZoneId zone) {
        var time = TIME.format(cancellation.startsAt().atZone(zone));
        return english(language)
                ? "Not enough people joined. It was at %s · %s.".formatted(time, cancellation.placeName())
                : "No se llegó al mínimo de participantes. Era a las %s · %s.".formatted(time,
                cancellation.placeName());
    }

    private static boolean english(String language) {
        return "en".equals(language);
    }
}

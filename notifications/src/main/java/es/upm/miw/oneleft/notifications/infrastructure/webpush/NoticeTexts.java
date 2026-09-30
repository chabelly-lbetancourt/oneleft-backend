package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;

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
    private static final Map<Activity, String> SPANISH = Map.of(
            Activity.PADEL, "Pádel", Activity.FOOTBALL, "Fútbol", Activity.BASKETBALL, "Baloncesto",
            Activity.TENNIS, "Tenis", Activity.RUNNING, "Running", Activity.CYCLING, "Ciclismo",
            Activity.HIKING, "Senderismo", Activity.BOARD_GAMES, "Juegos de mesa", Activity.CINEMA, "Cine",
            Activity.CONCERTS, "Conciertos");
    private static final Map<Activity, String> ENGLISH = Map.of(
            Activity.PADEL, "Padel", Activity.FOOTBALL, "Football", Activity.BASKETBALL, "Basketball",
            Activity.TENNIS, "Tennis", Activity.RUNNING, "Running", Activity.CYCLING, "Cycling",
            Activity.HIKING, "Hiking", Activity.BOARD_GAMES, "Board games", Activity.CINEMA, "Cinema",
            Activity.CONCERTS, "Concerts");

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

    private static boolean english(String language) {
        return "en".equals(language);
    }
}

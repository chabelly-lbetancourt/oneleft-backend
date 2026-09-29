package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.infrastructure.config.ShareProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Page behind a shared plan link (HU-024). WhatsApp, Telegram or social networks read its Open Graph tags without
 * running JavaScript, so the preview (activity, time, free spots) must come from the server; people are sent on to
 * the plan in the web app. Every value from the plan is HTML-escaped: the title is written by users.
 */
@Component
class SharePage {

    private static final Map<Activity, String[]> ACTIVITIES = Map.of(
            Activity.PADEL, new String[]{"Pádel", "Padel"},
            Activity.FOOTBALL, new String[]{"Fútbol", "Football"},
            Activity.BASKETBALL, new String[]{"Baloncesto", "Basketball"},
            Activity.TENNIS, new String[]{"Tenis", "Tennis"},
            Activity.RUNNING, new String[]{"Running", "Running"},
            Activity.CYCLING, new String[]{"Ciclismo", "Cycling"},
            Activity.HIKING, new String[]{"Senderismo", "Hiking"},
            Activity.BOARD_GAMES, new String[]{"Juegos de mesa", "Board games"},
            Activity.CINEMA, new String[]{"Cine", "Cinema"},
            Activity.CONCERTS, new String[]{"Conciertos", "Concerts"});

    private final ShareProperties properties;

    SharePage(ShareProperties properties) {
        this.properties = properties;
    }

    /** Preview of a plan, in Spanish or English. */
    String of(Plan plan, Locale locale) {
        var english = isEnglish(locale);
        var activity = ACTIVITIES.get(plan.activity())[english ? 1 : 0];
        var time = DateTimeFormatter.ofPattern("HH:mm").withZone(properties.zone()).format(plan.startsAt());
        var spots = switch (plan.freeSpots()) {
            case 0 -> english ? "Full" : "Completo";
            case 1 -> english ? "1 spot left" : "Falta 1";
            default -> english ? plan.freeSpots() + " spots left" : "Faltan " + plan.freeSpots();
        };
        var description = activity + " · " + (english ? "at " : "a las ") + time + " · " + spots + " · "
                + plan.meetingPoint().name();
        return page(english, plan.title(), description, planUrl(plan.id()));
    }

    /** The plan does not exist (any more): the link goes to the home screen. */
    String missing(Locale locale) {
        var english = isEnglish(locale);
        return page(english, english ? "This plan is no longer available" : "Este plan ya no está disponible",
                english ? "Find other plans near you on OneLeft" : "Encuentra otros planes cerca en OneLeft",
                properties.webUrl().toString());
    }

    String planUrl(UUID planId) {
        return properties.webUrl().resolve("/plans/" + planId).toString();
    }

    private String page(boolean english, String title, String description, String target) {
        var image = properties.webUrl().resolve("/og-image.png").toString();
        var html = """
                <!doctype html>
                <html lang="%s">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s · OneLeft</title>
                <meta name="description" content="%s">
                <meta property="og:type" content="website">
                <meta property="og:site_name" content="OneLeft">
                <meta property="og:title" content="%s">
                <meta property="og:description" content="%s">
                <meta property="og:url" content="%s">
                <meta property="og:image" content="%s">
                <meta property="og:image:width" content="1200">
                <meta property="og:image:height" content="630">
                <meta name="twitter:card" content="summary_large_image">
                <meta http-equiv="refresh" content="0; url=%s">
                <link rel="canonical" href="%s">
                </head>
                <body>
                <p><a href="%s">%s</a></p>
                </body>
                </html>
                """;
        return html.formatted(english ? "en" : "es", escape(title), escape(description), escape(title),
                escape(description), escape(target), escape(image), escape(target), escape(target), escape(target),
                english ? "Open the plan in OneLeft" : "Abrir el plan en OneLeft");
    }

    private static boolean isEnglish(Locale locale) {
        return locale != null && "en".equals(locale.getLanguage());
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }
}

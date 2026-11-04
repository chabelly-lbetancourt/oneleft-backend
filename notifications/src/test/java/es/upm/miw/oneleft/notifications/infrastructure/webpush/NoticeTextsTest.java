package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-068: a system notification can name every activity of the catalog, in both languages. */
class NoticeTextsTest {

    @Test
    void everyActivityHasANameInSpanishAndEnglish() {
        for (var activity : Activity.values()) {
            var notice = new NearbyPlanNotice(UUID.randomUUID(), UUID.randomUUID(), activity, "Plan", "Pistas",
                    Instant.parse("2026-11-16T17:00:00Z"), 1, 700);

            var spanish = NoticeTexts.body(notice, "es", ZoneId.of("Europe/Madrid"));
            var english = NoticeTexts.body(notice, "en", ZoneId.of("Europe/Madrid"));

            assertThat(spanish).as(activity.name()).doesNotContain("null");
            assertThat(english).as(activity.name()).doesNotContain("null");
        }
        assertThat(Arrays.stream(Activity.values())).hasSize(29);
    }

    @Test
    void theNewActivitiesAreNamedInTheLanguageOfTheSubscription() {
        var notice = new NearbyPlanNotice(UUID.randomUUID(), UUID.randomUUID(), Activity.MUSEUMS_EXHIBITIONS, "Visita",
                "Prado", Instant.parse("2026-11-16T17:00:00Z"), 2, 1500);

        assertThat(NoticeTexts.body(notice, "es", ZoneId.of("Europe/Madrid"))).startsWith("Museos y exposiciones a las");
        assertThat(NoticeTexts.body(notice, "en", ZoneId.of("Europe/Madrid"))).startsWith("Museums and exhibitions at");
    }
}

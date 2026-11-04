package es.upm.miw.oneleft.plans.infrastructure.rest;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.infrastructure.config.ShareProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.COURTS;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;

/** HU-068: the preview of a shared plan can name every activity of the catalog. */
class SharePageTest {

    private final SharePage page = new SharePage(new ShareProperties(URI.create("http://localhost:4200"),
            ZoneId.of("Europe/Madrid")));

    @Test
    void everyActivityHasANameInSpanishAndEnglish() {
        for (var activity : Arrays.stream(Activity.values()).filter(a -> a != Activity.OTHER).toList()) {
            var plan = Plan.publish(ana(), activity, "Quedada", null, COURTS, NOW.plus(Duration.ofHours(1)), 1, null,
                    CLOCK);
            assertThat(page.of(plan, Locale.forLanguageTag("es"))).as(activity.name()).contains("og:description");
            assertThat(page.of(plan, Locale.ENGLISH)).as(activity.name()).contains("og:description");
        }
    }

    @Test
    void anotherActivityNamesItselfWithTheTextOfThePlanAndEscapesIt() {
        var plan = Plan.publish(ana(), Activity.OTHER, "<b>Cerámica</b>", "Quedada", null, COURTS,
                NOW.plus(Duration.ofHours(1)), 1, null, null, null, CLOCK);

        var html = page.of(plan, Locale.forLanguageTag("es"));

        assertThat(html).contains("&lt;b&gt;Cerámica&lt;/b&gt;").doesNotContain("<b>Cerámica</b>");
    }
}

package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.DraftRequest;
import es.upm.miw.oneleft.plans.domain.model.DraftUnavailableException;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.Place;
import es.upm.miw.oneleft.plans.domain.model.PlaceQuery;
import es.upm.miw.oneleft.plans.domain.model.PlaceSuggestion;
import es.upm.miw.oneleft.plans.domain.model.RawDraft;
import es.upm.miw.oneleft.plans.domain.model.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-010: what the language model says is only a suggestion, checked against the rules of a plan. */
class PlanDraftServiceTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    /** Monday 16/11/2026 at 17:00 in Madrid */
    private static final Instant NOW = Instant.parse("2026-11-16T16:00:00Z");
    private static final DraftRequest REQUEST = DraftRequest.of("Pádel hoy a las 19 en la Albufera, falta uno", 40.39,
            -3.63, "Europe/Madrid");

    private RawDraft answer;
    private final List<DraftRequest> interpreted = new ArrayList<>();
    private final List<PlaceQuery> searched = new ArrayList<>();
    private List<PlaceSuggestion> found = List.of(new PlaceSuggestion(
            new Place("Pistas de pádel de la Albufera", "Madrid", 40.3964, -3.6297), 700));
    private final PlanDraftService service = new PlanDraftService((text, now, zone) -> {
        interpreted.add(new DraftRequest(text, 40, -3, zone));
        return answer;
    }, query -> {
        searched.add(query);
        return found;
    }, Clock.fixed(NOW, ZoneId.of("UTC")));

    private static RawDraft complete() {
        return new RawDraft("PADEL", "Pádel 2 contra 2, falta uno", "Nivel medio", "la Albufera",
                "2026-11-16T19:00", 1, "INTERMEDIATE", null);
    }

    @Test
    void aCompleteSentenceGivesACompleteDraft() {
        answer = complete();

        var draft = service.draft(REQUEST);

        assertThat(draft.activity()).isEqualTo(Activity.PADEL);
        assertThat(draft.title()).isEqualTo("Pádel 2 contra 2, falta uno");
        assertThat(draft.description()).isEqualTo("Nivel medio");
        assertThat(draft.level()).isEqualTo(Level.INTERMEDIATE);
        assertThat(draft.spots()).isEqualTo(1);
        // 19:00 in Madrid in November is 18:00 UTC
        assertThat(draft.startsAt()).isEqualTo(Instant.parse("2026-11-16T18:00:00Z"));
        assertThat(draft.place()).isEqualTo("la Albufera");
        assertThat(draft.meetingPoint().name()).isEqualTo("Pistas de pádel de la Albufera");
        assertThat(draft.meetingPoint().latitude()).isEqualTo(40.3964);
        assertThat(draft.missing()).isEmpty();
        assertThat(draft.warnings()).isEmpty();
        assertThat(interpreted).singleElement().satisfies(request -> assertThat(request.zone()).isEqualTo(MADRID));
        assertThat(searched).containsExactly(new PlaceQuery("la Albufera", 40.39, -3.63));
    }

    @Test
    void whatTheSentenceDoesNotSayIsReportedInsteadOfInvented() {
        answer = new RawDraft(null, null, null, null, null, null, null, null);

        var draft = service.draft(REQUEST);

        assertThat(draft.missing()).containsExactly("activity", "title", "meetingPoint", "startsAt", "spots");
        assertThat(draft.meetingPoint()).isNull();
        assertThat(searched).as("no place, nothing to look up").isEmpty();
    }

    @Test
    void anActivityOrLevelThatDoesNotExistIsDiscarded() {
        answer = new RawDraft("quidditch", "Partido", null, "la Albufera", "2026-11-16T19:00", 2, "expert", null);

        var draft = service.draft(REQUEST);

        assertThat(draft.activity()).isNull();
        assertThat(draft.level()).isNull();
        assertThat(draft.missing()).containsExactly("activity");
    }

    @Test
    void theActivityIsReadWhateverItsCase() {
        answer = new RawDraft(" padel ", "Partido", null, "la Albufera", "2026-11-16T19:00", 2, "advanced", null);

        var draft = service.draft(REQUEST);

        assertThat(draft.activity()).isEqualTo(Activity.PADEL);
        assertThat(draft.level()).isEqualTo(Level.ADVANCED);
    }

    @Test
    void aTimeOutsideTheNextTwelveHoursOrTooSoonIsNotAcceptedAndSaysWhy() {
        for (var startsAt : new String[]{"2026-11-17T19:00", "2026-11-16T17:03", "2026-11-16T10:00"}) {
            answer = new RawDraft("PADEL", "Partido", null, "la Albufera", startsAt, 1, null, null);

            var draft = service.draft(REQUEST);

            assertThat(draft.startsAt()).as(startsAt).isNull();
            assertThat(draft.missing()).as(startsAt).contains("startsAt");
            assertThat(draft.warnings()).as(startsAt).contains("startsAt.window");
        }
    }

    @Test
    void aTimeThatCannotBeReadIsMissingWithoutAWarning() {
        answer = new RawDraft("PADEL", "Partido", null, "la Albufera", "esta tarde", 1, null, null);

        var draft = service.draft(REQUEST);

        assertThat(draft.startsAt()).isNull();
        assertThat(draft.missing()).contains("startsAt");
        assertThat(draft.warnings()).isEmpty();
    }

    @Test
    void thePlaceIsLookedUpAndNeverTrusted() {
        found = List.of();
        answer = complete();

        var draft = service.draft(REQUEST);

        assertThat(draft.meetingPoint()).isNull();
        assertThat(draft.place()).as("what the person said is kept to show it").isEqualTo("la Albufera");
        assertThat(draft.missing()).containsExactly("meetingPoint");
        assertThat(draft.warnings()).containsExactly("meetingPoint.notFound");
    }

    @Test
    void aPlaceTooShortToSearchIsReportedAsNotFound() {
        answer = new RawDraft("PADEL", "Partido", null, "ab", "2026-11-16T19:00", 1, null, null);

        var draft = service.draft(REQUEST);

        assertThat(draft.missing()).containsExactly("meetingPoint");
        assertThat(draft.warnings()).containsExactly("meetingPoint.notFound");
        assertThat(searched).isEmpty();
    }

    @Test
    void textsAreCutToWhatAPlaceAllowsAndTooShortATitleIsMissing() {
        answer = new RawDraft("PADEL", "T".repeat(120), "D".repeat(400), "la Albufera", "2026-11-16T19:00", 1, null,
                null);
        assertThat(service.draft(REQUEST).title()).hasSize(80);
        assertThat(service.draft(REQUEST).description()).hasSize(280);

        answer = new RawDraft("PADEL", "ab", "  ", "la Albufera", "2026-11-16T19:00", 1, null, null);
        var draft = service.draft(REQUEST);
        assertThat(draft.missing()).containsExactly("title");
        assertThat(draft.description()).isNull();
    }

    @Test
    void spotsAndMinimumMustFitThePlan() {
        for (var spots : new Integer[]{0, -1, 21}) {
            answer = new RawDraft("PADEL", "Partido", null, "la Albufera", "2026-11-16T19:00", spots, null, 2);
            var draft = service.draft(REQUEST);
            assertThat(draft.spots()).as("spots " + spots).isNull();
            assertThat(draft.missing()).as("spots " + spots).containsExactly("spots");
            assertThat(draft.minParticipants()).as("no minimum without spots").isNull();
        }
        answer = new RawDraft("PADEL", "Partido", null, "la Albufera", "2026-11-16T19:00", 3, null, 2);
        assertThat(service.draft(REQUEST).minParticipants()).isEqualTo(2);
        answer = new RawDraft("PADEL", "Partido", null, "la Albufera", "2026-11-16T19:00", 3, null, 5);
        assertThat(service.draft(REQUEST).minParticipants()).as("more than the spots").isNull();
    }

    @Test
    void ifTheModelIsNotAvailableTheErrorGoesUp() {
        var down = new PlanDraftService((text, now, zone) -> {
            throw new DraftUnavailableException("down");
        }, query -> found, Clock.fixed(NOW, ZoneId.of("UTC")));

        assertThatThrownBy(() -> down.draft(REQUEST)).isInstanceOf(DraftUnavailableException.class);
    }

    @Test
    void theRequestIsValidatedBeforeAnythingIsAsked() {
        assertThatThrownBy(() -> DraftRequest.of("  ", 40, -3, null)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("characters");
        assertThatThrownBy(() -> DraftRequest.of("x".repeat(301), 40, -3, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DraftRequest.of("Pádel", 95, -3, null)).isInstanceOf(ValidationException.class);
        assertThat(DraftRequest.of("Pádel hoy", 40, -3, "Not/AZone").zone()).isEqualTo(DraftRequest.DEFAULT_ZONE);
        assertThat(DraftRequest.of("Pádel hoy", 40, -3, " ").zone()).isEqualTo(DraftRequest.DEFAULT_ZONE);
        assertThat(DraftRequest.of("Pádel hoy", 40, -3, "America/Bogota").zone())
                .isEqualTo(ZoneId.of("America/Bogota"));
    }
}

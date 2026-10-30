package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.DraftRequest;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.PlaceQuery;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanDraft;
import es.upm.miw.oneleft.plans.domain.model.RawDraft;
import es.upm.miw.oneleft.plans.domain.model.ValidationException;
import es.upm.miw.oneleft.plans.domain.port.in.DraftPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.in.SuggestPlacesUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.PlanInterpreter;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Turns a sentence into a draft of a plan (HU-010). The language model only suggests: every field is checked against
 * the rules of a plan, what does not fit is left out and reported, and the meeting point is looked up with the place
 * search (HU-071) instead of trusting any coordinates. The person reviews the draft and publishes it as usual.
 */
@Service
public class PlanDraftService implements DraftPlanUseCase {

    static final String ACTIVITY = "activity";
    static final String TITLE = "title";
    static final String MEETING_POINT = "meetingPoint";
    static final String STARTS_AT = "startsAt";
    static final String SPOTS = "spots";
    /** The time is understood but OneLeft is for plans in the next few hours. */
    static final String STARTS_AT_WINDOW = "startsAt.window";
    static final String PLACE_NOT_FOUND = "meetingPoint.notFound";

    private final PlanInterpreter interpreter;
    private final SuggestPlacesUseCase places;
    private final Clock clock;

    public PlanDraftService(PlanInterpreter interpreter, SuggestPlacesUseCase places, Clock clock) {
        this.interpreter = interpreter;
        this.places = places;
        this.clock = clock;
    }

    @Override
    public PlanDraft draft(DraftRequest request) {
        var now = clock.instant();
        var raw = interpreter.interpret(request.text(), now, request.zone());
        var missing = new ArrayList<String>();
        var warnings = new ArrayList<String>();

        var activity = activity(raw.activity());
        var title = text(raw.title(), Plan.MAX_TITLE_LENGTH);
        var spots = raw.spots() != null && raw.spots() >= 1 && raw.spots() <= Plan.MAX_SPOTS ? raw.spots() : null;
        var startsAt = startsAt(raw.startsAt(), request.zone(), now, warnings);
        var place = text(raw.place(), PlaceQuery.MAX_LENGTH);
        var meetingPoint = place == null ? null : meetingPoint(place, request, warnings).orElse(null);

        require(missing, ACTIVITY, activity);
        require(missing, TITLE, title == null || title.length() < Plan.MIN_TITLE_LENGTH ? null : title);
        require(missing, MEETING_POINT, meetingPoint);
        require(missing, STARTS_AT, startsAt);
        require(missing, SPOTS, spots);
        return new PlanDraft(activity, title, text(raw.description(), Plan.MAX_DESCRIPTION_LENGTH), place,
                meetingPoint, startsAt, spots, level(raw.level()), minParticipants(raw.minParticipants(), spots),
                missing, warnings);
    }

    private static void require(List<String> missing, String field, Object value) {
        if (value == null) {
            missing.add(field);
        }
    }

    /** The text without surrounding spaces, cut to the longest the plan allows; {@code null} if empty. */
    private static String text(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var stripped = value.strip();
        return stripped.length() > maxLength ? stripped.substring(0, maxLength).strip() : stripped;
    }

    private static Activity activity(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Activity.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Level level(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Level.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** At least 1 and, if the spots are known, no more than they are. */
    private static Integer minParticipants(Integer value, Integer spots) {
        return value != null && value >= 1 && spots != null && value <= spots ? value : null;
    }

    private static Instant startsAt(String value, ZoneId zone, Instant now, List<String> warnings) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            var startsAt = LocalDateTime.parse(value.strip()).atZone(zone).toInstant();
            if (startsAt.isBefore(now.plus(Plan.MIN_LEAD_TIME)) || startsAt.isAfter(now.plus(Plan.MAX_HORIZON))) {
                warnings.add(STARTS_AT_WINDOW);
                return null;
            }
            return startsAt;
        } catch (DateTimeException e) {
            return null;
        }
    }

    private Optional<MeetingPoint> meetingPoint(String place, DraftRequest request, List<String> warnings) {
        try {
            var found = places.suggest(new PlaceQuery(place, request.latitude(), request.longitude()));
            if (found.isEmpty()) {
                warnings.add(PLACE_NOT_FOUND);
                return Optional.empty();
            }
            var closest = found.getFirst().place();
            return Optional.of(new MeetingPoint(closest.name(), closest.latitude(), closest.longitude()));
        } catch (ValidationException e) {
            warnings.add(PLACE_NOT_FOUND);
            return Optional.empty();
        }
    }
}

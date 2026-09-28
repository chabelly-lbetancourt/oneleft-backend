package es.upm.miw.oneleft.plans.domain.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Main aggregate: a plan for the next few hours with free spots that other people can take.
 */
public class Plan {

    /** Minimum lead time so that someone can find out and get there. */
    public static final Duration MIN_LEAD_TIME = Duration.ofMinutes(5);
    /** OneLeft is for plans "right now": at most 12 hours ahead. */
    public static final Duration MAX_HORIZON = Duration.ofHours(12);
    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MAX_TITLE_LENGTH = 80;
    public static final int MAX_DESCRIPTION_LENGTH = 280;
    public static final int MAX_SPOTS = 20;

    private final UUID id;
    private final Organizer organizer;
    private final Activity activity;
    private final String title;
    private final String description;
    private final MeetingPoint meetingPoint;
    private final Instant startsAt;
    private final int spots;
    private final int occupied;
    private final Level level;
    private final PlanStatus status;
    private final Instant publishedAt;

    @SuppressWarnings("java:S107") // Full reconstruction of the aggregate from persistence
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt) {
        if (id == null || organizer == null || activity == null || meetingPoint == null || startsAt == null
                || status == null || publishedAt == null) {
            throw new ValidationException("plan.missingData", "Required plan data is missing");
        }
        if (title == null || title.strip().length() < MIN_TITLE_LENGTH || title.strip().length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("plan.title",
                    "The title must be between " + MIN_TITLE_LENGTH + " and " + MAX_TITLE_LENGTH + " characters");
        }
        if (description != null && description.strip().length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("plan.description",
                    "The description allows up to " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (spots < 1 || spots > MAX_SPOTS) {
            throw new ValidationException("plan.spots", "Free spots must be between 1 and " + MAX_SPOTS);
        }
        if (occupied < 0 || occupied > spots) {
            throw new ValidationException("plan.occupied", "Occupied spots cannot exceed the plan's spots");
        }
        this.id = id;
        this.organizer = organizer;
        this.activity = activity;
        this.title = title.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.meetingPoint = meetingPoint;
        this.startsAt = startsAt;
        this.spots = spots;
        this.occupied = occupied;
        this.level = level;
        this.status = status;
        this.publishedAt = publishedAt;
    }

    /**
     * Publishes a new plan. The start time must be between {@link #MIN_LEAD_TIME} and
     * {@link #MAX_HORIZON} from now: OneLeft is for plans in the next few hours.
     */
    @SuppressWarnings("java:S107")
    public static Plan publish(Organizer organizer, Activity activity, String title, String description,
                               MeetingPoint meetingPoint, Instant startsAt, int spots, Level level, Clock clock) {
        var now = clock.instant();
        if (startsAt == null || startsAt.isBefore(now.plus(MIN_LEAD_TIME))) {
            throw new ValidationException("plan.startsTooSoon", "The plan must start in at least "
                    + MIN_LEAD_TIME.toMinutes() + " minutes");
        }
        if (startsAt.isAfter(now.plus(MAX_HORIZON))) {
            throw new ValidationException("plan.startsTooLate", "The plan must start within the next "
                    + MAX_HORIZON.toHours() + " hours");
        }
        return new Plan(UUID.randomUUID(), organizer, activity, title, description, meetingPoint, startsAt, spots,
                0, level, PlanStatus.OPEN, now);
    }

    public int freeSpots() {
        return spots - occupied;
    }

    public PlanPublished publishedEvent() {
        return new PlanPublished(id, organizer.id(), activity, meetingPoint.latitude(), meetingPoint.longitude(),
                startsAt, freeSpots(), level, publishedAt);
    }

    public UUID id() {
        return id;
    }

    public Organizer organizer() {
        return organizer;
    }

    public Activity activity() {
        return activity;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public MeetingPoint meetingPoint() {
        return meetingPoint;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public int spots() {
        return spots;
    }

    public int occupied() {
        return occupied;
    }

    public Level level() {
        return level;
    }

    public PlanStatus status() {
        return status;
    }

    public Instant publishedAt() {
        return publishedAt;
    }
}

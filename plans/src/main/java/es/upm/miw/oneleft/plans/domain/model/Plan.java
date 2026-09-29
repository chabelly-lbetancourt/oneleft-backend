package es.upm.miw.oneleft.plans.domain.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    /** People who can wait for a spot of a full plan (HU-023). */
    public static final int MAX_WAITLIST = 10;

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
    private final List<Participant> participants;
    /** People waiting for a spot, in order of arrival (HU-023). */
    private final List<Participant> waitlist;
    /** Concurrency token of the persisted aggregate (optimistic locking). */
    private final long version;

    @SuppressWarnings("java:S107")
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt) {
        this(id, organizer, activity, title, description, meetingPoint, startsAt, spots, occupied, level, status,
                publishedAt, List.of(), 0);
    }

    @SuppressWarnings("java:S107")
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, long version) {
        this(id, organizer, activity, title, description, meetingPoint, startsAt, spots, occupied, level, status,
                publishedAt, participants, List.of(), version);
    }

    @SuppressWarnings("java:S107") // Full reconstruction of the aggregate from persistence
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, List<Participant> waitlist,
                long version) {
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
        this.participants = participants == null ? List.of() : List.copyOf(participants);
        this.waitlist = waitlist == null ? List.of() : List.copyOf(waitlist);
        this.version = version;
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

    /**
     * Takes a free spot (HU-005). The plan must be open and not started, and the person can be neither the organizer
     * nor someone already in it. Taking the last spot closes the plan ({@link PlanStatus#FULL}).
     */
    public Plan join(UUID userId, String name, Clock clock) {
        var now = clock.instant();
        if (organizer.id().equals(userId)) {
            throw new JoinRejectedException("plan.ownPlan", "The organizer cannot join their own plan");
        }
        if (participants.stream().anyMatch(participant -> participant.userId().equals(userId))) {
            throw new JoinRejectedException("plan.alreadyJoined", "You have already joined this plan");
        }
        if (!startsAt.isAfter(now)) {
            throw new JoinRejectedException("plan.started", "The plan has already started");
        }
        if (status == PlanStatus.FULL || freeSpots() == 0) {
            throw new JoinRejectedException("plan.full", "The plan has no free spots left");
        }
        if (status != PlanStatus.OPEN) {
            throw new JoinRejectedException("plan.notOpen", "The plan is no longer open");
        }
        var joined = new ArrayList<>(participants);
        joined.add(new Participant(userId, name, now));
        var newOccupied = occupied + 1;
        return new Plan(id, organizer, activity, title, description, meetingPoint, startsAt, spots, newOccupied, level,
                newOccupied == spots ? PlanStatus.FULL : PlanStatus.OPEN, publishedAt, joined, waitlist, version);
    }

    /**
     * Joins the waiting list of a full plan (HU-023): when someone leaves, the first person of the list takes the
     * spot. A plan with free spots is joined directly, so its list stays empty.
     */
    public Plan joinWaitlist(UUID userId, String name, Clock clock) {
        var now = clock.instant();
        if (organizer.id().equals(userId)) {
            throw new JoinRejectedException("plan.ownPlan", "The organizer cannot join their own plan");
        }
        if (isParticipant(userId)) {
            throw new JoinRejectedException("plan.alreadyJoined", "You have already joined this plan");
        }
        if (isWaiting(userId)) {
            throw new JoinRejectedException("plan.alreadyWaiting", "You are already on the waiting list");
        }
        if (!startsAt.isAfter(now)) {
            throw new JoinRejectedException("plan.started", "The plan has already started");
        }
        if (status == PlanStatus.OPEN && freeSpots() > 0) {
            throw new JoinRejectedException("plan.notFull", "The plan has free spots: join it directly");
        }
        if (status != PlanStatus.FULL) {
            throw new JoinRejectedException("plan.notOpen", "The plan is no longer open");
        }
        if (waitlist.size() == MAX_WAITLIST) {
            throw new JoinRejectedException("plan.waitlistFull", "The waiting list is full");
        }
        var waiting = new ArrayList<>(waitlist);
        waiting.add(new Participant(userId, name, now));
        return new Plan(id, organizer, activity, title, description, meetingPoint, startsAt, spots, occupied, level,
                status, publishedAt, participants, waiting, version);
    }

    /**
     * Leaves the plan before it starts (HU-023). The spot goes to the first person of the waiting list; if nobody is
     * waiting, it becomes free again and a full plan reopens.
     */
    public PlanLeft leave(UUID userId, Clock clock) {
        var now = clock.instant();
        var leaving = participants.stream().filter(participant -> participant.userId().equals(userId)).findFirst()
                .orElseThrow(() -> new JoinRejectedException("plan.notParticipant", "You have not joined this plan"));
        if (!startsAt.isAfter(now)) {
            throw new JoinRejectedException("plan.started", "The plan has already started");
        }
        var remaining = new ArrayList<>(participants);
        remaining.remove(leaving);
        Participant promoted = null;
        var waiting = new ArrayList<>(waitlist);
        var newOccupied = occupied - 1;
        if (!waiting.isEmpty()) {
            var first = waiting.removeFirst();
            promoted = new Participant(first.userId(), first.name(), now);
            remaining.add(promoted);
            newOccupied = occupied;
        }
        var plan = new Plan(id, organizer, activity, title, description, meetingPoint, startsAt, spots, newOccupied,
                level, newOccupied == spots ? PlanStatus.FULL : PlanStatus.OPEN, publishedAt, remaining, waiting,
                version);
        return new PlanLeft(plan, leaving, promoted, now);
    }

    /** Leaves the waiting list (HU-023). */
    public Plan leaveWaitlist(UUID userId) {
        if (!isWaiting(userId)) {
            throw new JoinRejectedException("plan.notWaiting", "You are not on the waiting list");
        }
        var waiting = waitlist.stream().filter(person -> !person.userId().equals(userId)).toList();
        return new Plan(id, organizer, activity, title, description, meetingPoint, startsAt, spots, occupied, level,
                status, publishedAt, participants, waiting, version);
    }

    /** Event of the last join: who joined, how many spots are left and whether the plan is now full. */
    public PlanJoined joinedEvent() {
        var last = participants.getLast();
        return new PlanJoined(id, organizer.id(), title, last.userId(), last.name(), freeSpots(),
                status == PlanStatus.FULL, last.joinedAt());
    }

    public boolean isParticipant(UUID userId) {
        return participants.stream().anyMatch(participant -> participant.userId().equals(userId));
    }

    public boolean isWaiting(UUID userId) {
        return waitlist.stream().anyMatch(person -> person.userId().equals(userId));
    }

    public List<Participant> participants() {
        return participants;
    }

    public List<Participant> waitlist() {
        return waitlist;
    }

    public long version() {
        return version;
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

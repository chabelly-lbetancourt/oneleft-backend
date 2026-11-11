package es.upm.miw.oneleft.plans.domain.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public static final int MAX_ACTIVITY_OTHER_LENGTH = 30;
    /** People who can wait for a spot of a full plan (HU-023). */
    public static final int MAX_WAITLIST = 10;
    /** Plans have no end time: they are over this long after they start (HU-007, «in progress» → «finished»). */
    public static final Duration DURATION = Duration.ofHours(3);
    /** Whoever is in the plan is reminded this long before it starts (HU-007). */
    public static final Duration REMINDER_LEAD = Duration.ofMinutes(30);

    private final UUID id;
    private final Organizer organizer;
    private final Activity activity;
    /** The name of the activity when it is {@link Activity#OTHER} (HU-068); null for any other. */
    private final String activityOther;
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
    /** When the reminder was sent (HU-007); null while it is pending. */
    private final Instant remindedAt;
    /** Minimum of participants and its deadline (HU-039); null when the plan goes ahead with anyone. */
    private final Minimum minimum;
    /** «On my way» and «running late» of the group (HU-040); emptied when the plan starts. */
    private final List<Arrival> arrivals;
    /** The organizer chooses who comes: nobody joins directly, they ask and the organizer decides (HU-069). */
    private final boolean requiresApproval;
    /** Requests for a spot, oldest first (HU-069); only for plans that require approval. */
    private final List<JoinRequest> requests;
    /** For whom the plan is, by gender and age (HU-070). */
    private final Audience audience;
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

    @SuppressWarnings("java:S107")
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, List<Participant> waitlist,
                long version) {
        this(id, organizer, activity, title, description, meetingPoint, startsAt, spots, occupied, level, status,
                publishedAt, participants, waitlist, null, version);
    }

    @SuppressWarnings("java:S107")
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, List<Participant> waitlist,
                Instant remindedAt, long version) {
        this(id, organizer, activity, title, description, meetingPoint, startsAt, spots, occupied, level, status,
                publishedAt, participants, waitlist, remindedAt, null, version);
    }

    @SuppressWarnings("java:S107")
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, List<Participant> waitlist,
                Instant remindedAt, Minimum minimum, long version) {
        this(id, organizer, activity, title, description, meetingPoint, startsAt, spots, occupied, level, status,
                publishedAt, participants, waitlist, remindedAt, minimum, List.of(), version);
    }

    @SuppressWarnings("java:S107")
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, List<Participant> waitlist,
                Instant remindedAt, Minimum minimum, List<Arrival> arrivals, long version) {
        this(id, organizer, activity, null, title, description, meetingPoint, startsAt, spots, occupied, level, status,
                publishedAt, participants, waitlist, remindedAt, minimum, arrivals, version);
    }

    @SuppressWarnings("java:S107")
    public Plan(UUID id, Organizer organizer, Activity activity, String activityOther, String title,
                String description, MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, List<Participant> waitlist,
                Instant remindedAt, Minimum minimum, List<Arrival> arrivals, long version) {
        this(id, organizer, activity, activityOther, title, description, meetingPoint, startsAt, spots, occupied,
                level, status, publishedAt, participants, waitlist, remindedAt, minimum, arrivals, false, List.of(),
                version);
    }

    @SuppressWarnings("java:S107") // Full reconstruction of the aggregate from persistence
    public Plan(UUID id, Organizer organizer, Activity activity, String activityOther, String title,
                String description, MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, List<Participant> waitlist,
                Instant remindedAt, Minimum minimum, List<Arrival> arrivals, boolean requiresApproval,
                List<JoinRequest> requests, long version) {
        this(id, organizer, activity, activityOther, title, description, meetingPoint, startsAt, spots, occupied,
                level, status, publishedAt, participants, waitlist, remindedAt, minimum, arrivals, requiresApproval,
                requests, Audience.EVERYONE, version);
    }

    @SuppressWarnings("java:S107") // Full reconstruction of the aggregate from persistence
    public Plan(UUID id, Organizer organizer, Activity activity, String activityOther, String title,
                String description, MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt, List<Participant> participants, List<Participant> waitlist,
                Instant remindedAt, Minimum minimum, List<Arrival> arrivals, boolean requiresApproval,
                List<JoinRequest> requests, Audience audience, long version) {
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
        if (minimum != null && (minimum.participants() < 1 || minimum.participants() > spots)) {
            throw new ValidationException("plan.minimum", "The minimum of participants must be between 1 and the "
                    + "plan's spots");
        }
        if (minimum != null && minimum.deadline().isAfter(startsAt)) {
            throw new ValidationException("plan.minimumDeadline", "The deadline of the minimum cannot be after the "
                    + "start of the plan");
        }
        this.id = id;
        this.organizer = organizer;
        this.activity = activity;
        this.activityOther = activityOther(activity, activityOther);
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
        this.remindedAt = remindedAt;
        this.minimum = minimum;
        this.arrivals = arrivals == null ? List.of() : List.copyOf(arrivals);
        this.requiresApproval = requiresApproval;
        this.requests = requests == null ? List.of() : List.copyOf(requests);
        this.audience = audience == null ? Audience.EVERYONE : audience;
        this.version = version;
    }

    /**
     * The name of an activity that is «other» (HU-068): required, at most {@link #MAX_ACTIVITY_OTHER_LENGTH}
     * characters and no control characters. For any other activity it is not kept. Moderation of the text (HU-011)
     * is still to be plugged in here.
     */
    private static String activityOther(Activity activity, String text) {
        if (activity != Activity.OTHER) {
            return null;
        }
        var name = text == null ? "" : text.strip();
        if (name.isEmpty() || name.length() > MAX_ACTIVITY_OTHER_LENGTH || name.chars().anyMatch(Character::isISOControl)) {
            throw new ValidationException("plan.activityOther", "«Other» needs a name of up to "
                    + MAX_ACTIVITY_OTHER_LENGTH + " characters");
        }
        return name;
    }

    /**
     * Publishes a new plan. The start time must be between {@link #MIN_LEAD_TIME} and
     * {@link #MAX_HORIZON} from now: OneLeft is for plans in the next few hours.
     */
    @SuppressWarnings("java:S107")
    public static Plan publish(Organizer organizer, Activity activity, String title, String description,
                               MeetingPoint meetingPoint, Instant startsAt, int spots, Level level, Clock clock) {
        return publish(organizer, activity, title, description, meetingPoint, startsAt, spots, level, null, null,
                clock);
    }

    /**
     * Publishes a new plan with an optional minimum of participants (HU-039): {@code minParticipants} people must
     * have joined by {@code minimumDeadline}, which is at least {@link #MIN_LEAD_TIME} from now and not after the
     * start. Both or neither.
     */
    @SuppressWarnings("java:S107")
    public static Plan publish(Organizer organizer, Activity activity, String title, String description,
                               MeetingPoint meetingPoint, Instant startsAt, int spots, Level level,
                               Integer minParticipants, Instant minimumDeadline, Clock clock) {
        return publish(organizer, activity, null, title, description, meetingPoint, startsAt, spots, level,
                minParticipants, minimumDeadline, clock);
    }

    /** The same, for an activity that may be «other» (HU-068) and then carries its own name. */
    @SuppressWarnings("java:S107")
    public static Plan publish(Organizer organizer, Activity activity, String activityOther, String title,
                               String description, MeetingPoint meetingPoint, Instant startsAt, int spots,
                               Level level, Integer minParticipants, Instant minimumDeadline, Clock clock) {
        return publish(organizer, activity, activityOther, title, description, meetingPoint, startsAt, spots, level,
                minParticipants, minimumDeadline, false, clock);
    }

    /** The same, for a plan whose organizer chooses who comes (HU-069): nobody joins directly, they ask. */
    @SuppressWarnings("java:S107")
    public static Plan publish(Organizer organizer, Activity activity, String activityOther, String title,
                               String description, MeetingPoint meetingPoint, Instant startsAt, int spots,
                               Level level, Integer minParticipants, Instant minimumDeadline,
                               boolean requiresApproval, Clock clock) {
        return publish(organizer, activity, activityOther, title, description, meetingPoint, startsAt, spots, level,
                minParticipants, minimumDeadline, requiresApproval, Audience.EVERYONE, clock);
    }

    /** The same, for a plan that is for some people only (HU-070). */
    @SuppressWarnings("java:S107")
    public static Plan publish(Organizer organizer, Activity activity, String activityOther, String title,
                               String description, MeetingPoint meetingPoint, Instant startsAt, int spots,
                               Level level, Integer minParticipants, Instant minimumDeadline,
                               boolean requiresApproval, Audience audience, Clock clock) {
        var now = clock.instant();
        if (startsAt == null || startsAt.isBefore(now.plus(MIN_LEAD_TIME))) {
            throw new ValidationException("plan.startsTooSoon", "The plan must start in at least "
                    + MIN_LEAD_TIME.toMinutes() + " minutes");
        }
        if (startsAt.isAfter(now.plus(MAX_HORIZON))) {
            throw new ValidationException("plan.startsTooLate", "The plan must start within the next "
                    + MAX_HORIZON.toHours() + " hours");
        }
        // A plan published with less than the reminder lead needs no reminder: everyone is just finding out about it
        var remindedAt = startsAt.minus(REMINDER_LEAD).isAfter(now) ? null : now;
        return new Plan(UUID.randomUUID(), organizer, activity, activityOther, title, description, meetingPoint,
                startsAt, spots, 0, level, PlanStatus.OPEN, now, List.of(), List.of(), remindedAt,
                minimum(minParticipants, minimumDeadline, now), List.of(), requiresApproval, List.of(), audience, 0);
    }

    private static Minimum minimum(Integer participants, Instant deadline, Instant now) {
        if (participants == null && deadline == null) {
            return null;
        }
        if (participants == null || deadline == null) {
            throw new ValidationException("plan.minimumDeadline", "The minimum of participants and its deadline go "
                    + "together");
        }
        if (deadline.isBefore(now.plus(MIN_LEAD_TIME))) {
            throw new ValidationException("plan.minimumDeadlineTooSoon", "The deadline of the minimum must be at "
                    + "least " + MIN_LEAD_TIME.toMinutes() + " minutes from now");
        }
        return new Minimum(participants, deadline, null);
    }

    /**
     * Takes a free spot (HU-005). The plan must be open and not started, and the person can be neither the organizer
     * nor someone already in it. Taking the last spot closes the plan ({@link PlanStatus#FULL}).
     */
    public Plan join(UUID userId, String name, Clock clock) {
        if (requiresApproval) {
            throw new JoinRejectedException("plan.approvalRequired", "The organizer chooses who comes: ask for a spot");
        }
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
        return with(newOccupied, newOccupied == spots ? PlanStatus.FULL : PlanStatus.OPEN, joined, waitlist,
                remindedAt, minimum);
    }

    /**
     * Joins the waiting list of a full plan (HU-023): when someone leaves, the first person of the list takes the
     * spot. A plan with free spots is joined directly, so its list stays empty.
     */
    public Plan joinWaitlist(UUID userId, String name, Clock clock) {
        if (requiresApproval) {
            throw new JoinRejectedException("plan.approvalRequired", "The organizer chooses who comes: ask for a spot");
        }
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
        return with(occupied, status, participants, waiting, remindedAt, minimum);
    }

    /**
     * Asks for a spot in a plan whose organizer chooses who comes (HU-069). Someone whose request was rejected cannot
     * ask again; whoever withdrew can.
     */
    public Plan request(UUID userId, String name, String message, Clock clock) {
        var now = clock.instant();
        if (!requiresApproval) {
            throw new JoinRejectedException("plan.approvalNotRequired", "This plan is joined directly");
        }
        if (organizer.id().equals(userId)) {
            throw new JoinRejectedException("plan.ownPlan", "The organizer cannot join their own plan");
        }
        if (isParticipant(userId)) {
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
        var previous = requestOf(userId);
        if (previous.isPresent() && previous.get().status() == RequestStatus.PENDING) {
            throw new JoinRejectedException("plan.alreadyRequested", "You have already asked for a spot");
        }
        if (previous.isPresent() && previous.get().status() == RequestStatus.REJECTED) {
            throw new JoinRejectedException("plan.requestRejected", "Your request for this plan was rejected");
        }
        var all = new ArrayList<>(requests.stream().filter(request -> !request.userId().equals(userId)).toList());
        all.add(JoinRequest.pending(userId, name, message, now));
        return withRequests(all);
    }

    /**
     * The organizer accepts a request (HU-069): the person takes a spot. If that fills the plan, the other pending
     * requests are rejected on their own.
     */
    public RequestDecision accept(UUID organizerId, UUID requesterId, Clock clock) {
        var now = clock.instant();
        var request = pendingRequest(organizerId, requesterId, now);
        if (status != PlanStatus.OPEN || freeSpots() == 0) {
            throw new JoinRejectedException("plan.full", "The plan has no free spots left");
        }
        var joined = new ArrayList<>(participants);
        joined.add(new Participant(requesterId, request.name(), now));
        var newOccupied = occupied + 1;
        var full = newOccupied == spots;
        var accepted = request.decided(RequestStatus.ACCEPTED, now);
        var autoRejected = new ArrayList<JoinRequest>();
        var all = new ArrayList<JoinRequest>();
        for (var other : requests) {
            if (other.userId().equals(requesterId)) {
                all.add(accepted);
            } else if (full && other.isPending()) {
                var rejected = other.decided(RequestStatus.REJECTED, now);
                autoRejected.add(rejected);
                all.add(rejected);
            } else {
                all.add(other);
            }
        }
        var plan = with(newOccupied, full ? PlanStatus.FULL : PlanStatus.OPEN, joined, waitlist, remindedAt, minimum)
                .withRequests(all);
        return new RequestDecision(plan, accepted, autoRejected);
    }

    /** The organizer rejects a request (HU-069). */
    public RequestDecision reject(UUID organizerId, UUID requesterId, Clock clock) {
        var now = clock.instant();
        var rejected = pendingRequest(organizerId, requesterId, now).decided(RequestStatus.REJECTED, now);
        return new RequestDecision(replacing(rejected), rejected, List.of());
    }

    /** The person withdraws their own pending request (HU-069). */
    public Plan withdraw(UUID userId, Clock clock) {
        var now = clock.instant();
        var request = requestOf(userId).filter(found -> found.isPending() && startsAt.isAfter(now))
                .orElseThrow(() -> new JoinRejectedException("plan.noPendingRequest", "You have no pending request"));
        return replacing(request.decided(RequestStatus.WITHDRAWN, now));
    }

    private JoinRequest pendingRequest(UUID organizerId, UUID requesterId, Instant now) {
        if (!organizer.id().equals(organizerId)) {
            throw new NotOrganizerException();
        }
        return requestOf(requesterId).filter(found -> found.isPending() && startsAt.isAfter(now))
                .orElseThrow(() -> new JoinRejectedException("plan.noPendingRequest", "There is no pending request"));
    }

    private Plan replacing(JoinRequest changed) {
        return withRequests(requests.stream()
                .map(request -> request.userId().equals(changed.userId()) ? changed : request).toList());
    }

    public Optional<JoinRequest> requestOf(UUID userId) {
        return requests.stream().filter(request -> request.userId().equals(userId)).findFirst();
    }

    /** The status of a request now: a pending one expires when the plan starts (HU-069). */
    public RequestStatus statusOf(JoinRequest request, Instant now) {
        return request.isPending() && !startsAt.isAfter(now) ? RequestStatus.EXPIRED : request.status();
    }

    public Audience audience() {
        return audience;
    }

    public boolean requiresApproval() {
        return requiresApproval;
    }

    public List<JoinRequest> requests() {
        return requests;
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
        var plan = with(newOccupied, newOccupied == spots ? PlanStatus.FULL : PlanStatus.OPEN, remaining, waiting,
                remindedAt, minimum, arrivalsWithout(userId));
        return new PlanLeft(plan, leaving, promoted, now);
    }

    /** Leaves the waiting list (HU-023). */
    public Plan leaveWaitlist(UUID userId) {
        if (!isWaiting(userId)) {
            throw new JoinRejectedException("plan.notWaiting", "You are not on the waiting list");
        }
        var waiting = waitlist.stream().filter(person -> !person.userId().equals(userId)).toList();
        return with(occupied, status, participants, waiting, remindedAt, minimum);
    }

    /**
     * Moves the plan along its lifecycle at {@code now} (HU-007, state diagram of the proposal): an open or full plan
     * is «in progress» from its start time, and «finished» {@link #DURATION} later. A plan that starts no longer
     * takes anyone, so its waiting list is emptied. Returns this same plan when nothing changes.
     */
    public Plan advance(Instant now) {
        var next = status;
        if ((next == PlanStatus.OPEN || next == PlanStatus.FULL) && !startsAt.isAfter(now)) {
            next = PlanStatus.IN_PROGRESS;
        }
        if (next == PlanStatus.IN_PROGRESS && !startsAt.plus(DURATION).isAfter(now)) {
            next = PlanStatus.FINISHED;
        }
        if (next == status) {
            return this;
        }
        // Statuses of arrival only make sense before the start (HU-040)
        return with(occupied, next, participants, List.of(), remindedAt, minimum, List.of());
    }

    /**
     * Whether the plan is about to start and its reminder has not been sent (HU-007). A plan with a minimum is only
     * reminded once confirmed (HU-039): nobody is reminded of a plan that may still be cancelled.
     */
    public boolean needsReminder(Instant now) {
        return (status == PlanStatus.OPEN || status == PlanStatus.FULL) && remindedAt == null
                && (minimum == null || !minimum.pending())
                && startsAt.isAfter(now) && !startsAt.minus(REMINDER_LEAD).isAfter(now);
    }

    /** Whether the deadline of the minimum has come and the plan is still waiting for it (HU-039). */
    public boolean needsMinimumCheck(Instant now) {
        return (status == PlanStatus.OPEN || status == PlanStatus.FULL) && minimum != null && minimum.pending()
                && !minimum.deadline().isAfter(now);
    }

    /**
     * Checks the minimum at its deadline (HU-039). With enough participants the plan is confirmed and goes ahead even
     * if someone leaves later. Otherwise it is cancelled, its waiting list is emptied and everyone in it (organizer,
     * participants and waiting list) is told.
     */
    public MinimumChecked checkMinimum(Instant now) {
        if (!needsMinimumCheck(now)) {
            throw new IllegalStateException("The plan " + id + " has no minimum to check");
        }
        if (occupied >= minimum.participants()) {
            return new MinimumChecked(with(occupied, status, participants, waitlist, remindedAt,
                    minimum.confirm(now)), Optional.empty());
        }
        var recipients = new ArrayList<UUID>();
        recipients.add(organizer.id());
        participants.forEach(participant -> recipients.add(participant.userId()));
        waitlist.forEach(person -> recipients.add(person.userId()));
        var cancelled = with(occupied, PlanStatus.CANCELLED, participants, List.of(), remindedAt, minimum,
                List.of());
        return new MinimumChecked(cancelled, Optional.of(new PlanCancelled(id, organizer.id(), title,
                meetingPoint.name(), startsAt, PlanCancelled.Reason.MINIMUM_NOT_REACHED, recipients, now)));
    }

    /**
     * Sends the reminder (HU-007): the organizer and everyone in the plan are told it is about to start. The plan
     * records it, so it is sent once.
     */
    public PlanReminded remind(Instant now) {
        if (!needsReminder(now)) {
            throw new IllegalStateException("The plan " + id + " does not need a reminder");
        }
        var recipients = new ArrayList<UUID>();
        recipients.add(organizer.id());
        participants.forEach(participant -> recipients.add(participant.userId()));
        var plan = with(occupied, status, participants, waitlist, now, minimum);
        return new PlanReminded(plan, new PlanReminder(id, title, meetingPoint.name(), startsAt, recipients, now));
    }

    public Instant remindedAt() {
        return remindedAt;
    }

    public Minimum minimum() {
        return minimum;
    }

    public List<Arrival> arrivals() {
        return arrivals;
    }

    /** Whether the person is in the group of the plan: its organizer or a participant. */
    public boolean isMember(UUID userId) {
        return organizer.id().equals(userId) || isParticipant(userId);
    }

    /**
     * Someone of the group says «on my way» or «running late» (HU-040), until the plan starts. A new announcement
     * replaces the previous one of the same person. The rest of the group is told.
     */
    public ArrivalAnnounced announce(UUID userId, ArrivalStatus arrivalStatus, Integer minutesLate, Clock clock) {
        var now = clock.instant();
        checkCanAnnounce(userId, now);
        var name = memberName(userId);
        var arrival = new Arrival(userId, name, arrivalStatus, minutesLate, now);
        var updated = new ArrayList<>(arrivalsWithout(userId));
        updated.add(arrival);
        return new ArrivalAnnounced(with(occupied, status, participants, waitlist, remindedAt, minimum, updated),
                new PlanArrival(id, title, userId, name, arrivalStatus, arrival.minutesLate(), groupBut(userId),
                        now));
    }

    /**
     * Someone of the group arrives at the meeting point (HU-025): from {@link CheckIn#OPENS_BEFORE} before the start
     * until {@link CheckIn#CLOSES_AFTER} after it, unless the plan was cancelled. Whether they are close enough is
     * checked outside, against the meeting point in the database.
     */
    public CheckIn checkIn(UUID userId, Clock clock) {
        var now = clock.instant();
        checkIsMember(userId);
        if (status == PlanStatus.CANCELLED) {
            throw new JoinRejectedException("plan.notOpen", "The plan was cancelled");
        }
        if (now.isBefore(startsAt.minus(CheckIn.OPENS_BEFORE))) {
            throw new JoinRejectedException("checkIn.notYet", "The check-in opens " + CheckIn.OPENS_BEFORE.toMinutes()
                    + " minutes before the start");
        }
        if (now.isAfter(startsAt.plus(CheckIn.CLOSES_AFTER))) {
            throw new JoinRejectedException("checkIn.closed", "The check-in closed " + CheckIn.CLOSES_AFTER.toMinutes()
                    + " minutes after the start");
        }
        return new CheckIn(id, userId, memberName(userId), now);
    }

    /** The event of a check-in, for the rest of the group. */
    /**
     * Who of the group arrived, once the check-in has closed (HU-025, HU-009): the check-ins of people who are no
     * longer in the group do not count.
     */
    public PlanAttendanceClosed attendance(List<CheckIn> checkIns, Instant now) {
        var group = new ArrayList<UUID>();
        group.add(organizer.id());
        participants.forEach(participant -> group.add(participant.userId()));
        var arrived = checkIns.stream().map(CheckIn::userId).collect(Collectors.toSet());
        return new PlanAttendanceClosed(id, title, activity, organizer.id(),
                group.stream().filter(arrived::contains).toList(),
                group.stream().filter(userId -> !arrived.contains(userId)).toList(), freeSpots() == 0, startsAt, now);
    }

    public PlanCheckedIn checkedInEvent(CheckIn checkIn) {
        return new PlanCheckedIn(id, title, checkIn.userId(), checkIn.name(), groupBut(checkIn.userId()),
                checkIn.at());
    }

    /** Throws unless the person is the organizer or a participant. */
    public void checkIsMember(UUID userId) {
        if (!isMember(userId)) {
            throw new JoinRejectedException("plan.notParticipant", "You have not joined this plan");
        }
    }

    private String memberName(UUID userId) {
        return organizer.id().equals(userId) ? organizer.name() : participants.stream()
                .filter(participant -> participant.userId().equals(userId)).findFirst().orElseThrow().name();
    }

    /** The organizer and the participants, except {@code userId}. */
    private List<UUID> groupBut(UUID userId) {
        var recipients = new ArrayList<UUID>();
        if (!organizer.id().equals(userId)) {
            recipients.add(organizer.id());
        }
        participants.stream().map(Participant::userId).filter(member -> !member.equals(userId))
                .forEach(recipients::add);
        return recipients;
    }

    /** Takes back the own status of arrival (HU-040). */
    public Plan clearArrival(UUID userId, Clock clock) {
        checkCanAnnounce(userId, clock.instant());
        return with(occupied, status, participants, waitlist, remindedAt, minimum, arrivalsWithout(userId));
    }

    private void checkCanAnnounce(UUID userId, Instant now) {
        checkIsMember(userId);
        if (!startsAt.isAfter(now) || status != PlanStatus.OPEN && status != PlanStatus.FULL) {
            throw new JoinRejectedException("plan.started", "The plan has already started");
        }
    }

    private List<Arrival> arrivalsWithout(UUID userId) {
        return arrivals.stream().filter(arrival -> !arrival.userId().equals(userId)).toList();
    }

    /** Copy of this plan with what an operation changes; the rest, including the version, stays. */
    @SuppressWarnings("java:S107")
    private Plan with(int newOccupied, PlanStatus newStatus, List<Participant> newParticipants,
                      List<Participant> newWaitlist, Instant newRemindedAt, Minimum newMinimum) {
        return with(newOccupied, newStatus, newParticipants, newWaitlist, newRemindedAt, newMinimum, arrivals);
    }

    @SuppressWarnings("java:S107")
    private Plan with(int newOccupied, PlanStatus newStatus, List<Participant> newParticipants,
                      List<Participant> newWaitlist, Instant newRemindedAt, Minimum newMinimum,
                      List<Arrival> newArrivals) {
        return new Plan(id, organizer, activity, activityOther, title, description, meetingPoint, startsAt, spots,
                newOccupied, level, newStatus, publishedAt, newParticipants, newWaitlist, newRemindedAt, newMinimum,
                newArrivals, requiresApproval, requests, audience, version);
    }

    /** Copy of this plan with other requests; the rest stays. */
    private Plan withRequests(List<JoinRequest> newRequests) {
        return new Plan(id, organizer, activity, activityOther, title, description, meetingPoint, startsAt, spots,
                occupied, level, status, publishedAt, participants, waitlist, remindedAt, minimum, arrivals,
                requiresApproval, newRequests, audience, version);
    }

    /** Event of the last join: who joined, how many spots are left and whether the plan is now full. */
    public PlanJoined joinedEvent() {
        var last = participants.getLast();
        return new PlanJoined(id, organizer.id(), title, activity, startsAt, last.userId(), last.name(), freeSpots(),
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
        return new PlanPublished(id, organizer.id(), activity, title, meetingPoint.name(), meetingPoint.latitude(),
                meetingPoint.longitude(),
                startsAt, freeSpots(), level, publishedAt);
    }

    public UUID id() {
        return id;
    }

    public Organizer organizer() {
        return organizer;
    }

    /** The name of the activity when it is «other», or null. */
    public String activityOther() {
        return activityOther;
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

    /** When the plan is over: {@link #DURATION} after its start (HU-007), and its chat closes (HU-008). */
    public Instant endsAt() {
        return startsAt.plus(DURATION);
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

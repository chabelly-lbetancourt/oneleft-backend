package es.upm.miw.oneleft.plans.domain.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Agregado principal: un plan para las próximas horas con plazas libres que otras personas pueden ocupar.
 */
public class Plan {

    /** Margen mínimo para que alguien pueda enterarse y llegar. */
    public static final Duration MIN_LEAD_TIME = Duration.ofMinutes(5);
    /** OneLeft es para planes «para ya»: como mucho, 12 horas vista. */
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

    @SuppressWarnings("java:S107") // Reconstrucción completa del agregado desde la persistencia
    public Plan(UUID id, Organizer organizer, Activity activity, String title, String description,
                MeetingPoint meetingPoint, Instant startsAt, int spots, int occupied, Level level,
                PlanStatus status, Instant publishedAt) {
        if (id == null || organizer == null || activity == null || meetingPoint == null || startsAt == null
                || status == null || publishedAt == null) {
            throw new IllegalArgumentException("Faltan datos obligatorios del plan");
        }
        if (title == null || title.strip().length() < MIN_TITLE_LENGTH || title.strip().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "El título debe tener entre " + MIN_TITLE_LENGTH + " y " + MAX_TITLE_LENGTH + " caracteres");
        }
        if (description != null && description.strip().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("La descripción admite hasta " + MAX_DESCRIPTION_LENGTH + " caracteres");
        }
        if (spots < 1 || spots > MAX_SPOTS) {
            throw new IllegalArgumentException("Las plazas libres deben estar entre 1 y " + MAX_SPOTS);
        }
        if (occupied < 0 || occupied > spots) {
            throw new IllegalArgumentException("Las plazas ocupadas no pueden superar las plazas del plan");
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
     * Publica un plan nuevo. La hora de inicio debe estar entre {@link #MIN_LEAD_TIME} y
     * {@link #MAX_HORIZON}: OneLeft es para planes de las próximas horas.
     */
    @SuppressWarnings("java:S107")
    public static Plan publish(Organizer organizer, Activity activity, String title, String description,
                               MeetingPoint meetingPoint, Instant startsAt, int spots, Level level, Clock clock) {
        var now = clock.instant();
        if (startsAt == null || startsAt.isBefore(now.plus(MIN_LEAD_TIME))) {
            throw new IllegalArgumentException("El plan debe empezar dentro de al menos "
                    + MIN_LEAD_TIME.toMinutes() + " minutos");
        }
        if (startsAt.isAfter(now.plus(MAX_HORIZON))) {
            throw new IllegalArgumentException("El plan debe empezar dentro de las próximas "
                    + MAX_HORIZON.toHours() + " horas");
        }
        return new Plan(UUID.randomUUID(), organizer, activity, title, description, meetingPoint, startsAt, spots,
                0, level, PlanStatus.ABIERTO, now);
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

package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plan")
public class PlanEntity {

    @Id
    private UUID id;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(name = "organizer_name", nullable = false, length = 50)
    private String organizerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Activity activity;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 280)
    private String description;

    @Column(name = "meeting_point", nullable = false, length = 100)
    private String meetingPoint;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private int spots;

    @Column(nullable = false)
    private int occupied;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Version
    private long version;

    /** Loaded in batches, so that listing 50 nearby plans does not run 50 extra queries. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plan_participant", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderBy("joinedAt")
    @BatchSize(size = 50)
    private List<ParticipantEmbeddable> participants = new ArrayList<>();

    protected PlanEntity() {
    }

    @SuppressWarnings("java:S107")
    PlanEntity(UUID id, UUID organizerId, String organizerName, Activity activity, String title, String description,
               String meetingPoint, Point location, Instant startsAt, int spots, int occupied, Level level,
               PlanStatus status, Instant publishedAt, List<ParticipantEmbeddable> participants, long version) {
        this.id = id;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.activity = activity;
        this.title = title;
        this.description = description;
        this.meetingPoint = meetingPoint;
        this.location = location;
        this.startsAt = startsAt;
        this.spots = spots;
        this.occupied = occupied;
        this.level = level;
        this.status = status;
        this.publishedAt = publishedAt;
        this.participants = new ArrayList<>(participants);
        this.version = version;
    }

    UUID getId() { return id; }
    UUID getOrganizerId() { return organizerId; }
    String getOrganizerName() { return organizerName; }
    Activity getActivity() { return activity; }
    String getTitle() { return title; }
    String getDescription() { return description; }
    String getMeetingPoint() { return meetingPoint; }
    Point getLocation() { return location; }
    Instant getStartsAt() { return startsAt; }
    int getSpots() { return spots; }
    int getOccupied() { return occupied; }
    Level getLevel() { return level; }
    PlanStatus getStatus() { return status; }
    Instant getPublishedAt() { return publishedAt; }
    List<ParticipantEmbeddable> getParticipants() { return participants; }
    long getVersion() { return version; }
}

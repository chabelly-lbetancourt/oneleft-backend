package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Someone in a plan whose attendance closed (HU-044). Written with a native insert that ignores a repeated event;
 * read to know who may give kudos to whom (HU-060).
 */
@Entity
@Table(name = "track_record")
public class TrackRecordEntity {

    @EmbeddedId
    private Key id;

    @Column(nullable = false, length = 30)
    private String activity;

    @Column(nullable = false)
    private boolean organizer;

    @Column(nullable = false)
    private boolean arrived;

    @Column(nullable = false)
    private boolean held;

    @Column(name = "closed_at", nullable = false)
    private Instant closedAt;

    protected TrackRecordEntity() {
    }

    Attendance toDomain() {
        return new Attendance(id.userId(), id.planId(), activity, organizer, arrived, held, closedAt);
    }

    /** Who ({@code userId}) in which plan. */
    @Embeddable
    public record Key(@Column(name = "user_id") UUID userId,
                      @Column(name = "plan_id") UUID planId) implements Serializable {
    }
}

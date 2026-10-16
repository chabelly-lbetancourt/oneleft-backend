package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.ActivityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Something someone did in a plan (HU-051). Written with a native insert that ignores repeated events. */
@Entity
@Table(name = "activity")
public class ActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivityType type;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "plan_title", nullable = false, length = 80)
    private String planTitle;

    @Column(name = "plan_activity", nullable = false, length = 30)
    private String planActivity;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ActivityEntity() {
    }
}

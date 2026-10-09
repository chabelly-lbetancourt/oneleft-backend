package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.Arrival;
import es.upm.miw.oneleft.plans.domain.model.ArrivalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.util.UUID;

@Embeddable
class ArrivalEmbeddable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false, length = 50)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArrivalStatus status;
    @Column(name = "minutes_late")
    private Integer minutesLate;
    @Column(name = "announced_at", nullable = false)
    private Instant announcedAt;

    protected ArrivalEmbeddable() {
    }

    ArrivalEmbeddable(Arrival arrival) {
        this.userId = arrival.userId();
        this.name = arrival.name();
        this.status = arrival.status();
        this.minutesLate = arrival.minutesLate();
        this.announcedAt = arrival.at();
    }

    Arrival toDomain() {
        return new Arrival(userId, name, status, minutesLate, announcedAt);
    }
}

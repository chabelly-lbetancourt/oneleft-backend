package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plan_check_in")
class CheckInEntity {

    @EmbeddedId
    private Key id;
    @Column(nullable = false, length = 50)
    private String name;
    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    protected CheckInEntity() {
    }

    CheckInEntity(CheckIn checkIn) {
        this.id = new Key(checkIn.planId(), checkIn.userId());
        this.name = checkIn.name();
        this.checkedInAt = checkIn.at();
    }

    CheckIn toDomain() {
        return new CheckIn(id.planId(), id.userId(), name, checkedInAt);
    }

    @Embeddable
    record Key(@Column(name = "plan_id") UUID planId, @Column(name = "user_id") UUID userId) implements Serializable {
    }
}

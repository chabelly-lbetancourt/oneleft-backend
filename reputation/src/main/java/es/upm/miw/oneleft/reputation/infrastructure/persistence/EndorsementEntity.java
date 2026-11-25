package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.Endorsement;
import es.upm.miw.oneleft.reputation.domain.model.SkillLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * An endorsement given (HU-061). Written with a native insert that ignores a second one of the same endorser, receiver
 * and activity.
 */
@Entity
@Table(name = "endorsement")
public class EndorsementEntity {

    @EmbeddedId
    private Key id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillLevel level;

    @Column(name = "given_at", nullable = false)
    private Instant givenAt;

    protected EndorsementEntity() {
    }

    Endorsement toDomain() {
        return new Endorsement(id.endorserId(), id.receiverId(), id.activity(), level, givenAt);
    }

    /** Who ({@code endorserId}) endorsed whom ({@code receiverId}) in which activity. */
    @Embeddable
    public record Key(@Column(name = "endorser_id") UUID endorserId,
                      @Column(name = "receiver_id") UUID receiverId,
                      @Column(name = "activity", length = 30) String activity) implements Serializable {
    }
}

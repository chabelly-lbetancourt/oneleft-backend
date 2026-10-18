package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.model.KudosKind;
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

/** A kudos given (HU-060). Written with a native insert that ignores a second one of the same giver and plan. */
@Entity
@Table(name = "kudos")
public class KudosEntity {

    @EmbeddedId
    private Key id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KudosKind kind;

    @Column(name = "given_at", nullable = false)
    private Instant givenAt;

    protected KudosEntity() {
    }

    Kudos toDomain() {
        return new Kudos(id.giverId(), id.receiverId(), id.planId(), kind, givenAt);
    }

    /** Who ({@code giverId}) gave a kudos to whom ({@code receiverId}) in which plan. */
    @Embeddable
    public record Key(@Column(name = "giver_id") UUID giverId,
                      @Column(name = "receiver_id") UUID receiverId,
                      @Column(name = "plan_id") UUID planId) implements Serializable {
    }
}

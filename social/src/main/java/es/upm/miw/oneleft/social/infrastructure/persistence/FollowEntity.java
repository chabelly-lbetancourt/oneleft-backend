package es.upm.miw.oneleft.social.infrastructure.persistence;

import es.upm.miw.oneleft.social.domain.model.Follow;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
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

@Entity
@Table(name = "follow")
public class FollowEntity {

    @EmbeddedId
    private Key id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FollowStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected FollowEntity() {
    }

    static FollowEntity of(Follow follow) {
        var entity = new FollowEntity();
        entity.id = new Key(follow.followerId(), follow.followeeId());
        entity.status = follow.status();
        entity.requestedAt = follow.requestedAt();
        entity.acceptedAt = follow.acceptedAt();
        return entity;
    }

    Follow toDomain() {
        return new Follow(id.followerId, id.followeeId, status, requestedAt, acceptedAt);
    }

    /** Who follows ({@code followerId}) whom ({@code followeeId}). */
    @Embeddable
    public record Key(@Column(name = "follower_id") UUID followerId,
                      @Column(name = "followee_id") UUID followeeId) implements Serializable {
    }
}

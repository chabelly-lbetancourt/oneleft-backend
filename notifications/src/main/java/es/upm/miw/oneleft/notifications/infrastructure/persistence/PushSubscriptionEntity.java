package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_subscription")
class PushSubscriptionEntity {

    @Id
    private String endpoint;
    @Column(name = "user_id")
    private UUID userId;
    private String p256dh;
    private String auth;
    private String language;
    @Column(name = "created_at")
    private Instant createdAt;

    protected PushSubscriptionEntity() {
    }

    static PushSubscriptionEntity of(PushSubscription subscription, Instant now) {
        var entity = new PushSubscriptionEntity();
        entity.endpoint = subscription.endpoint();
        entity.userId = subscription.userId();
        entity.p256dh = subscription.p256dh();
        entity.auth = subscription.auth();
        entity.language = subscription.language();
        entity.createdAt = now;
        return entity;
    }

    PushSubscription toDomain() {
        return new PushSubscription(userId, endpoint, p256dh, auth, language);
    }
}

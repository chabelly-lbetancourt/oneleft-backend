package es.upm.miw.oneleft.social.infrastructure.messaging;

import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.port.out.FeedNotices;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Output adapter: new activity is a notice for each follower on the {@code oneleft.notifications} exchange, which the
 * plans service delivers through their real-time stream, like the badges (HU-044). Inside a transaction the notices
 * wait for the commit: activity that is not saved is never announced.
 */
@Component
public class RabbitFeedNotices implements FeedNotices {

    private final RabbitTemplate rabbit;

    public RabbitFeedNotices(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void newActivity(Collection<UUID> followers, FeedItem item) {
        if (followers.isEmpty()) {
            return;
        }
        var notices = followers.stream().map(follower -> SocialMessages.FeedActivity.of(follower, item)).toList();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(notices);
                }
            });
        } else {
            send(notices);
        }
    }

    private void send(List<SocialMessages.FeedActivity> notices) {
        notices.forEach(notice ->
                rabbit.convertAndSend(RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.FEED_ACTIVITY, notice));
    }
}

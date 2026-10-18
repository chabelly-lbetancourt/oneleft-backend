package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;
import es.upm.miw.oneleft.reputation.domain.port.out.KudosNotices;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Output adapter: a kudos received is a notice on the {@code oneleft.notifications} exchange, which the plans service
 * delivers through the real-time stream of the person, like the badges (HU-044). Inside a transaction the notice waits
 * for the commit: a kudos that is not saved is never announced.
 */
@Component
public class RabbitKudosNotices implements KudosNotices {

    private final RabbitTemplate rabbit;

    public RabbitKudosNotices(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void received(Kudos kudos, String giverName) {
        var notice = new ReputationMessages.KudosReceived(kudos.receiverId(), giverName, kudos.kind().name(),
                kudos.planId(), kudos.givenAt());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(notice);
                }
            });
        } else {
            send(notice);
        }
    }

    private void send(ReputationMessages.KudosReceived notice) {
        rabbit.convertAndSend(RabbitConfig.NOTIFICATIONS_EXCHANGE, RabbitConfig.KUDOS_RECEIVED, notice);
    }
}

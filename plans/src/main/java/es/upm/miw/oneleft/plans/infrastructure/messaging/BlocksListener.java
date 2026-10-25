package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.port.in.KeepBlocksUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Input adapter: the blocks of the social service (HU-033). Both events share one durable queue, so a block and the
 * unblock that follows it are copied in that order, once, by any replica.
 */
@Component
public class BlocksListener {

    private final KeepBlocksUseCase blocks;

    public BlocksListener(KeepBlocksUseCase blocks) {
        this.blocks = blocks;
    }

    @RabbitListener(queues = RabbitConfig.BLOCKS_QUEUE)
    public void onBlock(Block event, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        if (RabbitConfig.USER_UNBLOCKED.equals(routingKey)) {
            blocks.unblocked(event.blockerId(), event.blockedId());
        } else {
            blocks.blocked(event.blockerId(), event.blockedId(), event.occurredAt());
        }
    }

    /** user.blocked and user.unblocked: the part this service reads. */
    public record Block(UUID blockerId, UUID blockedId, Instant occurredAt) {
    }
}

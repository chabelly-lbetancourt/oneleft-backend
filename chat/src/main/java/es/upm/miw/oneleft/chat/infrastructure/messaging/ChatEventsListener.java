package es.upm.miw.oneleft.chat.infrastructure.messaging;

import es.upm.miw.oneleft.chat.infrastructure.websocket.ChatSessions;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter of the events that change the open chats. Anonymous queues: every replica handles its own
 * connections.
 */
@Component
public class ChatEventsListener {

    private final ChatSessions sessions;

    public ChatEventsListener(ChatSessions sessions) {
        this.sessions = sessions;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.CHAT_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.CHAT_MESSAGE))
    public void onMessage(ChatMessageEvent event) {
        sessions.deliver(event.toDomain());
    }

    /** Whoever leaves the plan is no longer in its chat. */
    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.PLANS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.PLAN_LEFT))
    public void onPlanLeft(PlanEvents.Left event) {
        sessions.leave(event.planId(), event.participantId());
    }

    /** A cancelled plan keeps its messages but nobody writes any more. */
    @RabbitListener(bindings = @QueueBinding(value = @Queue,
            exchange = @Exchange(name = RabbitConfig.PLANS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitConfig.PLAN_CANCELLED))
    public void onPlanCancelled(PlanEvents.Cancelled event) {
        sessions.close(event.planId());
    }
}

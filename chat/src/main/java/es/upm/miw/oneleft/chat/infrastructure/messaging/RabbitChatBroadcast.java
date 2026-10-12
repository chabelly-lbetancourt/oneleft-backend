package es.upm.miw.oneleft.chat.infrastructure.messaging;

import es.upm.miw.oneleft.chat.domain.model.ChatMessage;
import es.upm.miw.oneleft.chat.domain.port.out.ChatBroadcast;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Output adapter: a new message goes to every replica, which hands it to the people connected to its chat. */
@Component
public class RabbitChatBroadcast implements ChatBroadcast {

    private final RabbitTemplate rabbit;

    public RabbitChatBroadcast(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void send(ChatMessage message) {
        rabbit.convertAndSend(RabbitConfig.CHAT_EXCHANGE, RabbitConfig.CHAT_MESSAGE, ChatMessageEvent.of(message));
    }
}

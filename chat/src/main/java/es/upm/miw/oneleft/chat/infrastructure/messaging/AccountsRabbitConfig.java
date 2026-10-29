package es.upm.miw.oneleft.chat.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The deletion of accounts (HU-045): the users service announces {@code user.deleted} on {@code oneleft.accounts}
 * and each service erases its part from its own durable queue, so that it is handled once and not lost if the service
 * is down.
 */
@Configuration
public class AccountsRabbitConfig {

    public static final String ACCOUNTS_EXCHANGE = "oneleft.accounts";
    public static final String USER_DELETED = "user.deleted";
    public static final String ACCOUNT_DELETED_QUEUE = "chat.account-deleted";

    @Bean
    TopicExchange accountsExchange() {
        return new TopicExchange(ACCOUNTS_EXCHANGE, true, false);
    }

    @Bean
    Queue accountDeletedQueue() {
        return QueueBuilder.durable(ACCOUNT_DELETED_QUEUE).build();
    }

    @Bean
    Binding accountDeletedBinding(Queue accountDeletedQueue, TopicExchange accountsExchange) {
        return BindingBuilder.bind(accountDeletedQueue).to(accountsExchange).with(USER_DELETED);
    }
}

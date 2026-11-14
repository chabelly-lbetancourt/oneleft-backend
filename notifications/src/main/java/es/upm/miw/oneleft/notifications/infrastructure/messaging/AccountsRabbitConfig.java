package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The deletion of accounts (HU-045): the users service announces {@code user.deleted} (and {@code demographics.changed}, HU-070) on {@code oneleft.accounts}
 * and each service erases its part from its own durable queue, so that it is handled once and not lost if the service
 * is down.
 */
@Configuration
public class AccountsRabbitConfig {

    public static final String ACCOUNTS_EXCHANGE = "oneleft.accounts";
    public static final String USER_DELETED = "user.deleted";
    public static final String ACCOUNT_DELETED_QUEUE = "notifications.account-deleted";
    /** Gender and age of a person (HU-070): the notices of nearby plans leave out who the plan is not for. */
    public static final String DEMOGRAPHICS_CHANGED = "demographics.changed";
    public static final String DEMOGRAPHICS_QUEUE = "notifications.demographics";

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

    @Bean
    Queue demographicsQueue() {
        return QueueBuilder.durable(DEMOGRAPHICS_QUEUE).build();
    }

    @Bean
    Binding demographicsBinding(Queue demographicsQueue, TopicExchange accountsExchange) {
        return BindingBuilder.bind(demographicsQueue).to(accountsExchange).with(DEMOGRAPHICS_CHANGED);
    }
}

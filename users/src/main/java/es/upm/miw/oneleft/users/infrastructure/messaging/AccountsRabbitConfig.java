package es.upm.miw.oneleft.users.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The events of this service travel as JSON on {@code oneleft.accounts} (HU-045). */
@Configuration
public class AccountsRabbitConfig {

    public static final String ACCOUNTS_EXCHANGE = "oneleft.accounts";
    public static final String USER_DELETED = "user.deleted";
    /** Gender and age of a person (HU-070); the date of birth never travels. */
    public static final String DEMOGRAPHICS_CHANGED = "demographics.changed";

    @Bean
    TopicExchange accountsExchange() {
        return new TopicExchange(ACCOUNTS_EXCHANGE, true, false);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

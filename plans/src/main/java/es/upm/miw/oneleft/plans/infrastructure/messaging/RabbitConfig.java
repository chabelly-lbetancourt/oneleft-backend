package es.upm.miw.oneleft.plans.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** Topic exchange: each consumer subscribes to the events it is interested in. */
    public static final String PLANS_EXCHANGE = "oneleft.plans";
    public static final String PLAN_PUBLISHED = "plan.published";
    public static final String PLAN_JOINED = "plan.joined";

    @Bean
    TopicExchange plansExchange() {
        return new TopicExchange(PLANS_EXCHANGE, true, false);
    }

    /** Events travel as JSON so that any service can consume them. */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

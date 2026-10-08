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
    public static final String PLAN_LEFT = "plan.left";
    public static final String PLAN_REMINDER = "plan.reminder";
    public static final String PLAN_CANCELLED = "plan.cancelled";
    /** Notices of the notifications service, relayed to each person's real-time stream (HU-006). */
    public static final String NOTIFICATIONS_EXCHANGE = "oneleft.notifications";
    public static final String NEARBY_PLAN = "notification.nearby-plan";

    @Bean
    TopicExchange plansExchange() {
        return new TopicExchange(PLANS_EXCHANGE, true, false);
    }

    /**
     * Events travel as JSON so that any service can consume them. Each service reads them into its own classes (the
     * listener's parameter), never into the sender's: the JSON fields are the contract, not the Java types.
     */
    @Bean
    MessageConverter jsonMessageConverter() {
        var converter = new JacksonJsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}

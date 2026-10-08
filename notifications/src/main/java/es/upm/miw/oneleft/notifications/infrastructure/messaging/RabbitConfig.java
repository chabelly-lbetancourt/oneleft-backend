package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** Events of the plans service. */
    public static final String PLANS_EXCHANGE = "oneleft.plans";
    public static final String PLAN_PUBLISHED = "plan.published";
    /**
     * Durable queue shared by every replica of this service: each published plan is handled once, and plans published
     * while the service is down are processed when it comes back.
     */
    public static final String PLAN_PUBLISHED_QUEUE = "notifications.plan-published";
    public static final String PLAN_REMINDER = "plan.reminder";
    /** Durable and shared like the one above: each reminder (HU-007) is sent once, even by several replicas. */
    public static final String PLAN_REMINDER_QUEUE = "notifications.plan-reminder";
    public static final String PLAN_CANCELLED = "plan.cancelled";
    /** Durable and shared too: each cancellation (HU-039) is notified once. */
    public static final String PLAN_CANCELLED_QUEUE = "notifications.plan-cancelled";

    /** Notices for the app, relayed by the plans service through each person's real-time stream. */
    public static final String NOTIFICATIONS_EXCHANGE = "oneleft.notifications";
    public static final String NEARBY_PLAN = "notification.nearby-plan";

    @Bean
    TopicExchange plansExchange() {
        return new TopicExchange(PLANS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }

    @Bean
    Queue planPublishedQueue() {
        return QueueBuilder.durable(PLAN_PUBLISHED_QUEUE).build();
    }

    @Bean
    Binding planPublishedBinding(Queue planPublishedQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(planPublishedQueue).to(plansExchange).with(PLAN_PUBLISHED);
    }

    @Bean
    Queue planReminderQueue() {
        return QueueBuilder.durable(PLAN_REMINDER_QUEUE).build();
    }

    @Bean
    Binding planReminderBinding(Queue planReminderQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(planReminderQueue).to(plansExchange).with(PLAN_REMINDER);
    }

    @Bean
    Queue planCancelledQueue() {
        return QueueBuilder.durable(PLAN_CANCELLED_QUEUE).build();
    }

    @Bean
    Binding planCancelledBinding(Queue planCancelledQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(planCancelledQueue).to(plansExchange).with(PLAN_CANCELLED);
    }

    /**
     * Events travel as JSON. Each service reads them into its own classes (the listener's parameter), never into the
     * sender's: the JSON fields are the contract, not the Java types.
     */
    @Bean
    MessageConverter jsonMessageConverter() {
        var converter = new JacksonJsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}

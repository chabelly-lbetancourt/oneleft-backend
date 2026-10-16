package es.upm.miw.oneleft.social.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The events of the plans service that make the activity feed (HU-051). Each one has a durable queue shared by every
 * replica: it is handled once, and the events of while the service is down are handled when it comes back. An event
 * that keeps failing ends in {@code social.dead-letters}, to be looked at, instead of coming back forever.
 */
@Configuration
public class RabbitConfig {

    public static final String PLANS_EXCHANGE = "oneleft.plans";
    public static final String PLAN_PUBLISHED = "plan.published";
    public static final String PLAN_JOINED = "plan.joined";
    public static final String PLAN_LEFT = "plan.left";
    public static final String PLAN_CANCELLED = "plan.cancelled";
    public static final String PLAN_ATTENDANCE_CLOSED = "plan.attendance-closed";
    /** Notices for the app, relayed by the plans service through each person's real-time stream (HU-044). */
    public static final String NOTIFICATIONS_EXCHANGE = "oneleft.notifications";
    public static final String FEED_ACTIVITY = "notification.feed-activity";

    public static final String PUBLISHED_QUEUE = "social.published";
    public static final String JOINED_QUEUE = "social.joined";
    public static final String LEFT_QUEUE = "social.left";
    public static final String CANCELLED_QUEUE = "social.cancelled";
    public static final String ATTENDANCE_QUEUE = "social.attendance";
    public static final String DEAD_LETTERS_QUEUE = "social.dead-letters";

    private static Queue durable(String name) {
        return QueueBuilder.durable(name).deadLetterExchange("").deadLetterRoutingKey(DEAD_LETTERS_QUEUE).build();
    }

    @Bean
    TopicExchange plansExchange() {
        return new TopicExchange(PLANS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }

    @Bean
    Queue deadLettersQueue() {
        return QueueBuilder.durable(DEAD_LETTERS_QUEUE).build();
    }

    @Bean
    Queue publishedQueue() {
        return durable(PUBLISHED_QUEUE);
    }

    @Bean
    Binding publishedBinding(Queue publishedQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(publishedQueue).to(plansExchange).with(PLAN_PUBLISHED);
    }

    @Bean
    Queue joinedQueue() {
        return durable(JOINED_QUEUE);
    }

    @Bean
    Binding joinedBinding(Queue joinedQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(joinedQueue).to(plansExchange).with(PLAN_JOINED);
    }

    @Bean
    Queue leftQueue() {
        return durable(LEFT_QUEUE);
    }

    @Bean
    Binding leftBinding(Queue leftQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(leftQueue).to(plansExchange).with(PLAN_LEFT);
    }

    @Bean
    Queue cancelledQueue() {
        return durable(CANCELLED_QUEUE);
    }

    @Bean
    Binding cancelledBinding(Queue cancelledQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(cancelledQueue).to(plansExchange).with(PLAN_CANCELLED);
    }

    @Bean
    Queue attendanceQueue() {
        return durable(ATTENDANCE_QUEUE);
    }

    @Bean
    Binding attendanceBinding(Queue attendanceQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(attendanceQueue).to(plansExchange).with(PLAN_ATTENDANCE_CLOSED);
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

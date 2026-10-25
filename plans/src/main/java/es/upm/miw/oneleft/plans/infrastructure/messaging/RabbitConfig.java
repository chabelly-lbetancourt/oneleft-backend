package es.upm.miw.oneleft.plans.infrastructure.messaging;

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

    /** Topic exchange: each consumer subscribes to the events it is interested in. */
    public static final String PLANS_EXCHANGE = "oneleft.plans";
    public static final String PLAN_PUBLISHED = "plan.published";
    public static final String PLAN_JOINED = "plan.joined";
    public static final String PLAN_LEFT = "plan.left";
    public static final String PLAN_REMINDER = "plan.reminder";
    public static final String PLAN_CANCELLED = "plan.cancelled";
    public static final String PLAN_ARRIVAL = "plan.arrival";
    public static final String PLAN_CHECKED_IN = "plan.checked-in";
    public static final String PLAN_ATTENDANCE_CLOSED = "plan.attendance-closed";
    /** Notices of the notifications service, relayed to each person's real-time stream (HU-006). */
    public static final String NOTIFICATIONS_EXCHANGE = "oneleft.notifications";
    public static final String NEARBY_PLAN = "notification.nearby-plan";
    /** A new badge of reputation (HU-044), from the reputation service. */
    public static final String BADGE_EARNED = "notification.badge-earned";
    /** New activity of someone followed (HU-051), from the social service. */
    public static final String FEED_ACTIVITY = "notification.feed-activity";
    /** Kudos received (HU-060), from the reputation service */
    public static final String KUDOS = "notification.kudos";
    /** Blocks of the social service (HU-033), copied through a durable queue shared by every replica */
    public static final String SOCIAL_EXCHANGE = "oneleft.social";
    public static final String USER_BLOCKED = "user.blocked";
    public static final String USER_UNBLOCKED = "user.unblocked";
    public static final String BLOCKS_QUEUE = "plans.blocks";
    /** A copy that keeps failing ends here, to be looked at, instead of coming back forever */
    public static final String DEAD_LETTERS_QUEUE = "plans.dead-letters";

    @Bean
    TopicExchange plansExchange() {
        return new TopicExchange(PLANS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange socialExchange() {
        return new TopicExchange(SOCIAL_EXCHANGE, true, false);
    }

    @Bean
    Queue deadLettersQueue() {
        return QueueBuilder.durable(DEAD_LETTERS_QUEUE).build();
    }

    @Bean
    Queue blocksQueue() {
        return QueueBuilder.durable(BLOCKS_QUEUE).deadLetterExchange("").deadLetterRoutingKey(DEAD_LETTERS_QUEUE)
                .build();
    }

    @Bean
    Binding blockedBinding(Queue blocksQueue, TopicExchange socialExchange) {
        return BindingBuilder.bind(blocksQueue).to(socialExchange).with(USER_BLOCKED);
    }

    @Bean
    Binding unblockedBinding(Queue blocksQueue, TopicExchange socialExchange) {
        return BindingBuilder.bind(blocksQueue).to(socialExchange).with(USER_UNBLOCKED);
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

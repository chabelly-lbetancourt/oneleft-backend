package es.upm.miw.oneleft.reputation.infrastructure.messaging;

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
 * The events the reputation listens to (HU-009). Each one has a durable queue shared by every replica: it is handled
 * once, and the events of while the service is down are handled when it comes back. An event that keeps failing ends
 * in {@code reputation.dead-letters}, to be looked at, instead of coming back forever.
 */
@Configuration
public class RabbitConfig {

    /** Events of the plans service. */
    public static final String PLANS_EXCHANGE = "oneleft.plans";
    public static final String PLAN_CHECKED_IN = "plan.checked-in";
    public static final String PLAN_ATTENDANCE_CLOSED = "plan.attendance-closed";
    public static final String PLAN_CANCELLED = "plan.cancelled";
    /** Confirmed reports of the moderation (HU-033): the exchange is ready for when it publishes them. */
    public static final String REPORTS_EXCHANGE = "oneleft.reports";
    public static final String REPORT_CONFIRMED = "report.confirmed";
    /** Notices for the app, relayed by the plans service through each person's real-time stream (HU-044). */
    public static final String NOTIFICATIONS_EXCHANGE = "oneleft.notifications";
    public static final String BADGE_EARNED = "notification.badge-earned";

    public static final String CHECK_INS_QUEUE = "reputation.check-ins";
    public static final String ATTENDANCE_QUEUE = "reputation.attendance";
    public static final String CANCELLATIONS_QUEUE = "reputation.cancellations";
    public static final String REPORTS_QUEUE = "reputation.reports";
    public static final String DEAD_LETTERS_QUEUE = "reputation.dead-letters";

    private static Queue durable(String name) {
        return QueueBuilder.durable(name).deadLetterExchange("").deadLetterRoutingKey(DEAD_LETTERS_QUEUE).build();
    }

    @Bean
    TopicExchange plansExchange() {
        return new TopicExchange(PLANS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange reportsExchange() {
        return new TopicExchange(REPORTS_EXCHANGE, true, false);
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
    Queue checkInsQueue() {
        return durable(CHECK_INS_QUEUE);
    }

    @Bean
    Binding checkInsBinding(Queue checkInsQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(checkInsQueue).to(plansExchange).with(PLAN_CHECKED_IN);
    }

    @Bean
    Queue attendanceQueue() {
        return durable(ATTENDANCE_QUEUE);
    }

    @Bean
    Binding attendanceBinding(Queue attendanceQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(attendanceQueue).to(plansExchange).with(PLAN_ATTENDANCE_CLOSED);
    }

    @Bean
    Queue cancellationsQueue() {
        return durable(CANCELLATIONS_QUEUE);
    }

    @Bean
    Binding cancellationsBinding(Queue cancellationsQueue, TopicExchange plansExchange) {
        return BindingBuilder.bind(cancellationsQueue).to(plansExchange).with(PLAN_CANCELLED);
    }

    @Bean
    Queue reportsQueue() {
        return durable(REPORTS_QUEUE);
    }

    @Bean
    Binding reportsBinding(Queue reportsQueue, TopicExchange reportsExchange) {
        return BindingBuilder.bind(reportsQueue).to(reportsExchange).with(REPORT_CONFIRMED);
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

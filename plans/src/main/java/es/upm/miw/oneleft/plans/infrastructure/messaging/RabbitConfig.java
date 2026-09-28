package es.upm.miw.oneleft.plans.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** Exchange de tipo topic: cada consumidor se suscribe a los eventos que le interesan. */
    public static final String PLANS_EXCHANGE = "oneleft.plans";
    public static final String PLAN_PUBLISHED = "plan.published";

    @Bean
    TopicExchange plansExchange() {
        return new TopicExchange(PLANS_EXCHANGE, true, false);
    }

    /** Los eventos viajan en JSON para que cualquier servicio pueda consumirlos. */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

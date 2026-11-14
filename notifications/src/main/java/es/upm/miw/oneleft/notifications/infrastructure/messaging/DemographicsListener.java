package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.miw.oneleft.notifications.domain.port.in.KeepAudiencesUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Input adapter: the gender and age of people (HU-070), announced by the users service on its own durable queue, so
 * that each change is copied once, by any replica, even if this service was down when it happened.
 */
@Component
public class DemographicsListener {

    private final KeepAudiencesUseCase audiences;

    public DemographicsListener(KeepAudiencesUseCase audiences) {
        this.audiences = audiences;
    }

    @RabbitListener(queues = AccountsRabbitConfig.DEMOGRAPHICS_QUEUE)
    public void onChanged(DemographicsChanged event) {
        audiences.changed(event.userId(), event.gender(), event.age());
    }

    /** demographics.changed: the part this service reads. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DemographicsChanged(UUID userId, String gender, int age) {
    }
}

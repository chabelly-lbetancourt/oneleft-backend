package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.upm.miw.oneleft.notifications.domain.model.MemberPerks;
import es.upm.miw.oneleft.notifications.domain.port.in.KeepPerksUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Input adapter: what the level of each person is worth (HU-063), announced by the reputation service on a durable
 * queue of this one, so that each change is copied once, by any replica, even if this service was down when it happened.
 */
@Component
public class PerksListener {

    private final KeepPerksUseCase perks;

    public PerksListener(KeepPerksUseCase perks) {
        this.perks = perks;
    }

    @RabbitListener(queues = RabbitConfig.PERKS_QUEUE)
    public void onChanged(PerksChanged event) {
        perks.changed(new MemberPerks(event.userId(), event.level(), event.noticeWeight(), event.maxSavedAlerts(),
                event.occurredAt()));
    }

    /** reputation.perks-changed: the part this service reads. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PerksChanged(UUID userId, String level, double noticeWeight, int maxSavedAlerts,
                               Instant occurredAt) {
    }
}

package es.upm.miw.oneleft.plans.infrastructure.messaging;

import es.upm.miw.oneleft.plans.domain.port.in.PersonalDataUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Input adapter: someone deleted their account in the users service (HU-045), so this service erases its part. */
@Component
public class AccountDeletedListener {

    private final PersonalDataUseCase personalData;

    public AccountDeletedListener(PersonalDataUseCase personalData) {
        this.personalData = personalData;
    }

    @RabbitListener(queues = AccountsRabbitConfig.ACCOUNT_DELETED_QUEUE)
    public void onDeleted(AccountDeleted event) {
        personalData.erase(event.userId());
    }

    /** user.deleted: the part this service reads. */
    public record AccountDeleted(UUID userId, Instant occurredAt) {
    }
}

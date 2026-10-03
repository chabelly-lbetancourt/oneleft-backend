package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import es.upm.miw.oneleft.notifications.domain.port.in.RemindPlanUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: a plan is about to start (HU-007), so everyone in it gets a system notification.
 */
@Component
class PlanReminderListener {

    private final RemindPlanUseCase reminders;

    PlanReminderListener(RemindPlanUseCase reminders) {
        this.reminders = reminders;
    }

    @RabbitListener(queues = RabbitConfig.PLAN_REMINDER_QUEUE)
    void onPlanReminder(PlanReminderMessage event) {
        reminders.remind(event.toDomain());
    }
}

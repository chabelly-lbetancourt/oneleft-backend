package es.upm.miw.oneleft.social.infrastructure.messaging;

import es.upm.miw.oneleft.social.domain.port.in.RecordActivityUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Input adapter: each event of the plans service becomes activity in the feeds (HU-051). */
@Component
class SocialListeners {

    private final RecordActivityUseCase activity;

    SocialListeners(RecordActivityUseCase activity) {
        this.activity = activity;
    }

    @RabbitListener(queues = RabbitConfig.PUBLISHED_QUEUE)
    void onPublished(SocialMessages.Published event) {
        activity.record(event.toDomain());
    }

    @RabbitListener(queues = RabbitConfig.JOINED_QUEUE)
    void onJoined(SocialMessages.Joined event) {
        activity.record(event.toDomain());
    }

    @RabbitListener(queues = RabbitConfig.LEFT_QUEUE)
    void onLeft(SocialMessages.Left event) {
        activity.left(event.planId(), event.participantId());
        if (event.promotedId() != null) {
            activity.record(event.promoted());
        }
    }

    @RabbitListener(queues = RabbitConfig.CANCELLED_QUEUE)
    void onCancelled(SocialMessages.Cancelled event) {
        activity.cancelled(event.planId());
    }

    @RabbitListener(queues = RabbitConfig.ATTENDANCE_QUEUE)
    void onAttendance(SocialMessages.Attendance event) {
        event.toDomain().forEach(activity::record);
    }
}

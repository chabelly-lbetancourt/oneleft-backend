package es.upm.miw.oneleft.reputation.infrastructure.messaging;

import es.upm.miw.oneleft.reputation.domain.port.in.RecordReputationUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Input adapter: each event of plans and reports becomes entries of the ledger. */
@Component
class ReputationListeners {

    private final RecordReputationUseCase reputation;

    ReputationListeners(RecordReputationUseCase reputation) {
        this.reputation = reputation;
    }

    @RabbitListener(queues = RabbitConfig.CHECK_INS_QUEUE)
    void onCheckIn(ReputationMessages.CheckIn event) {
        reputation.record(event.toDomain());
    }

    @RabbitListener(queues = RabbitConfig.ATTENDANCE_QUEUE)
    void onAttendance(ReputationMessages.Attendance event) {
        reputation.record(event.toDomain());
    }

    @RabbitListener(queues = RabbitConfig.CANCELLATIONS_QUEUE)
    void onCancellation(ReputationMessages.Cancellation event) {
        reputation.record(event.toDomain());
    }

    @RabbitListener(queues = RabbitConfig.REPORTS_QUEUE)
    void onReport(ReputationMessages.Report event) {
        reputation.record(event.toDomain());
    }
}

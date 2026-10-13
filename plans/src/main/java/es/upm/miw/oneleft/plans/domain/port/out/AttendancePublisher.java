package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.PlanAttendanceClosed;

/** Tells the other services who arrived to a plan and who did not. */
public interface AttendancePublisher {

    void publish(PlanAttendanceClosed event);
}

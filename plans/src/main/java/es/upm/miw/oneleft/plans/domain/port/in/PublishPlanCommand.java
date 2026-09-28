package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;

import java.time.Instant;

public record PublishPlanCommand(Organizer organizer, Activity activity, String title, String description,
                                 MeetingPoint meetingPoint, Instant startsAt, int spots, Level level) {
}

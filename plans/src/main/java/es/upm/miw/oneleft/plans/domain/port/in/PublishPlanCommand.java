package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;

import java.time.Instant;

/**
 * Data of a plan to publish. {@code minParticipants} and {@code minimumDeadline} are the optional minimum of HU-039:
 * both or neither.
 */
public record PublishPlanCommand(Organizer organizer, Activity activity, String title, String description,
                                 MeetingPoint meetingPoint, Instant startsAt, int spots, Level level,
                                 Integer minParticipants, Instant minimumDeadline) {

    public PublishPlanCommand(Organizer organizer, Activity activity, String title, String description,
                              MeetingPoint meetingPoint, Instant startsAt, int spots, Level level) {
        this(organizer, activity, title, description, meetingPoint, startsAt, spots, level, null, null);
    }
}

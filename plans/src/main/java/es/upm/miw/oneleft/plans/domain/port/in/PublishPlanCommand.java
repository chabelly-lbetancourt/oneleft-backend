package es.upm.miw.oneleft.plans.domain.port.in;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Audience;
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
                                 Integer minParticipants, Instant minimumDeadline, String activityOther,
                                 boolean requiresApproval, Audience audience) {

    /** Without the choice of who comes (HU-069): everyone joins directly. */
    public PublishPlanCommand(Organizer organizer, Activity activity, String title, String description,
                              MeetingPoint meetingPoint, Instant startsAt, int spots, Level level,
                              Integer minParticipants, Instant minimumDeadline, String activityOther) {
        this(organizer, activity, title, description, meetingPoint, startsAt, spots, level, minParticipants,
                minimumDeadline, activityOther, false, Audience.EVERYONE);
    }

    /** Without the name of an activity that is «other». */
    public PublishPlanCommand(Organizer organizer, Activity activity, String title, String description,
                              MeetingPoint meetingPoint, Instant startsAt, int spots, Level level,
                              Integer minParticipants, Instant minimumDeadline) {
        this(organizer, activity, title, description, meetingPoint, startsAt, spots, level, minParticipants,
                minimumDeadline, null, false, Audience.EVERYONE);
    }

    public PublishPlanCommand(Organizer organizer, Activity activity, String title, String description,
                              MeetingPoint meetingPoint, Instant startsAt, int spots, Level level) {
        this(organizer, activity, title, description, meetingPoint, startsAt, spots, level, null, null, null, false, Audience.EVERYONE);
    }
}

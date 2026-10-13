package es.upm.miw.oneleft.reputation.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** The rules of the badges (HU-044): the track record each closed attendance adds, and the badges reached. */
public final class BadgeRules {

    private BadgeRules() {
    }

    /** One line of the track record for each person of the group */
    public static List<Attendance> attendances(AttendanceFact fact) {
        var lines = new ArrayList<Attendance>();
        fact.attendeeIds().forEach(userId -> lines.add(line(fact, userId, true)));
        fact.absenteeIds().forEach(userId -> lines.add(line(fact, userId, false)));
        return lines;
    }

    public static List<Badge> reached(TrackRecord record) {
        return Arrays.stream(Badge.values()).filter(record::reached).toList();
    }

    private static Attendance line(AttendanceFact fact, UUID userId, boolean arrived) {
        return new Attendance(userId, fact.planId(), fact.activity(), userId.equals(fact.organizerId()), arrived,
                fact.held(), fact.occurredAt());
    }
}

package es.upm.miw.oneleft.notifications.domain.model;

import java.time.LocalTime;

/**
 * Hours without notices, in local time. They may cross midnight (for example, 23:00 to 08:00).
 *
 * @param start first minute without notices
 * @param end   first minute with notices again
 */
public record QuietHours(LocalTime start, LocalTime end) {

    public QuietHours {
        if (start == null || end == null || start.equals(end)) {
            throw new ValidationException("notifications.quietHours", "Quiet hours need a start and a different end");
        }
    }

    public boolean includes(LocalTime time) {
        return start.isBefore(end)
                ? !time.isBefore(start) && time.isBefore(end)
                : !time.isBefore(start) || time.isBefore(end);
    }
}

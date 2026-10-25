package es.upm.miw.oneleft.social.infrastructure.messaging;

import es.upm.miw.oneleft.social.domain.model.Activity;
import es.upm.miw.oneleft.social.domain.model.ActivityType;
import es.upm.miw.oneleft.social.domain.model.FeedItem;
import es.upm.miw.oneleft.social.domain.model.Report;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The part of each event that the feed reads, and the events of the service: the JSON fields are the contract. */
final class SocialMessages {

    private SocialMessages() {
    }

    /** plan.published */
    record Published(UUID planId, UUID organizerId, String activity, String title, Instant startsAt,
                     Instant occurredAt) {

        Activity toDomain() {
            return new Activity(organizerId, ActivityType.PUBLISHED, planId, title, activity, startsAt, occurredAt);
        }
    }

    /** plan.joined */
    record Joined(UUID planId, String title, String activity, Instant startsAt, UUID participantId,
                  Instant occurredAt) {

        Activity toDomain() {
            return new Activity(participantId, ActivityType.JOINED, planId, title, activity, startsAt, occurredAt);
        }
    }

    /** plan.left: whoever came in from the waiting list has joined the plan */
    record Left(UUID planId, String title, String activity, Instant startsAt, UUID participantId, UUID promotedId,
                Instant occurredAt) {

        Activity promoted() {
            return new Activity(promotedId, ActivityType.JOINED, planId, title, activity, startsAt, occurredAt);
        }
    }

    /** plan.cancelled */
    record Cancelled(UUID planId) {
    }

    /** plan.attendance-closed: whoever arrived has completed the plan */
    record Attendance(UUID planId, String title, String activity, List<UUID> attendeeIds, Instant startsAt,
                      Instant occurredAt) {

        List<Activity> toDomain() {
            return attendeeIds.stream()
                    .map(attendee -> new Activity(attendee, ActivityType.COMPLETED, planId, title, activity, startsAt,
                            occurredAt))
                    .toList();
        }
    }

    /** user.blocked and user.unblocked (HU-033) */
    record Block(UUID blockerId, UUID blockedId, Instant occurredAt) {
    }

    /** report.created (HU-033), for the moderation (HU-011) */
    record ReportCreated(UUID reportId, UUID reporterId, String target, UUID targetId, UUID reportedUserId,
                         String reason, String comment, Instant occurredAt) {
        static ReportCreated of(Report report) {
            return new ReportCreated(report.id(), report.reporterId(), report.target().name(), report.targetId(),
                    report.reportedUserId(), report.reason().name(), report.comment(), report.reportedAt());
        }
    }

    /** notification.feed-activity, for the real-time stream of a follower, relayed by the plans service */
    record FeedActivity(UUID userId, UUID actorId, String actorName, String type, UUID planId, String planTitle,
                        Instant occurredAt) {

        static FeedActivity of(UUID follower, FeedItem item) {
            return new FeedActivity(follower, item.actorId(), item.actorName(), item.type().name(), item.planId(),
                    item.planTitle(), item.occurredAt());
        }
    }
}

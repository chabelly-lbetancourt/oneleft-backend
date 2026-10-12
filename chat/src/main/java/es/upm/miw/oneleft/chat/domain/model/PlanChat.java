package es.upm.miw.oneleft.chat.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * The chat of a plan as the plans service describes it: the organizer and the participants are its members, and it
 * is open until the plan ends (HU-007) unless it was cancelled before.
 *
 * @param endsAt when the plan is over: from then on the chat is read-only
 */
public record PlanChat(UUID planId, Set<UUID> members, Instant endsAt, boolean cancelled) {

    public PlanChat {
        members = Set.copyOf(members);
    }

    public boolean isMember(UUID userId) {
        return members.contains(userId);
    }

    public boolean isOpen(Instant now) {
        return !cancelled && now.isBefore(endsAt);
    }

    /** Checks that {@code userId} may read it. */
    public PlanChat readableBy(UUID userId) {
        if (!isMember(userId)) {
            throw new ChatException(ChatException.Reason.NOT_MEMBER, "Only the people of the plan are in its chat");
        }
        return this;
    }

    /** Checks that {@code userId} may write in it at {@code now}. */
    public PlanChat writableBy(UUID userId, Instant now) {
        readableBy(userId);
        if (!isOpen(now)) {
            throw new ChatException(ChatException.Reason.CLOSED, "The plan is over: its chat is read-only");
        }
        return this;
    }
}

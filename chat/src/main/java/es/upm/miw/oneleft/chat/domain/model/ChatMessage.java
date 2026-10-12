package es.upm.miw.oneleft.chat.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * A message in the chat of a plan (HU-008). It keeps when the plan ends, so that it can be deleted a while after.
 */
public record ChatMessage(UUID id, UUID planId, UUID authorId, String authorName, String text, Instant sentAt,
                          Instant planEndsAt) {

    public static final int MAX_LENGTH = 500;
    public static final int MAX_NAME_LENGTH = 100;

    public ChatMessage {
        text = text == null ? "" : text.strip();
        if (text.isEmpty() || text.length() > MAX_LENGTH) {
            throw new ValidationException("chat.text", "A message has between 1 and " + MAX_LENGTH + " characters");
        }
        authorName = authorName == null || authorName.isBlank() ? "?" : authorName.strip();
        if (authorName.length() > MAX_NAME_LENGTH) {
            authorName = authorName.substring(0, MAX_NAME_LENGTH);
        }
    }

    /** A new message of {@code authorId} in the chat, now. */
    public static ChatMessage write(PlanChat chat, UUID authorId, String authorName, String text, Clock clock) {
        var now = clock.instant();
        chat.writableBy(authorId, now);
        return new ChatMessage(UUID.randomUUID(), chat.planId(), authorId, authorName, text, now, chat.endsAt());
    }
}

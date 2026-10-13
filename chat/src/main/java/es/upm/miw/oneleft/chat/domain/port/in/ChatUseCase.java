package es.upm.miw.oneleft.chat.domain.port.in;

import es.upm.miw.oneleft.chat.domain.model.ChatHistory;
import es.upm.miw.oneleft.chat.domain.model.ChatMessage;
import es.upm.miw.oneleft.chat.domain.model.PlanChat;

import java.time.Instant;
import java.util.UUID;

/** The chat of a plan (HU-008): only its organizer and participants read it, and they write until the plan ends. */
public interface ChatUseCase {

    /** The chat of a plan for one of its members. */
    PlanChat open(UUID planId, UUID userId);

    /** The messages before {@code before} (the latest ones without it), oldest first, a page at a time. */
    ChatHistory history(UUID planId, UUID userId, Instant before);

    /** Writes in a chat already opened by its author: it is saved and reaches everyone connected to it. */
    ChatMessage send(PlanChat chat, UUID authorId, String authorName, String text);

    /** Deletes the messages of the plans that ended a while ago; returns how many. */
    int purge();
}

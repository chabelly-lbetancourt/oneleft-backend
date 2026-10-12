package es.upm.miw.oneleft.chat.domain.port.out;

import es.upm.miw.oneleft.chat.domain.model.ChatMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Output port to the messages of the chats. */
public interface ChatMessages {

    ChatMessage save(ChatMessage message);

    /** Up to {@code limit} messages of a plan sent before {@code before}, newest first. */
    List<ChatMessage> latest(UUID planId, Instant before, int limit);

    /** Deletes the messages of the plans that ended before {@code moment}; returns how many. */
    int deleteEndedBefore(Instant moment);
}

package es.upm.miw.oneleft.chat.domain.model;

import java.util.List;

/**
 * A page of the chat of a plan, oldest message first, and whether it can still be written in.
 */
public record ChatHistory(PlanChat chat, boolean open, List<ChatMessage> messages) {

    public ChatHistory {
        messages = List.copyOf(messages);
    }
}

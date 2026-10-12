package es.upm.miw.oneleft.chat.application;

import es.upm.miw.oneleft.chat.domain.model.ChatHistory;
import es.upm.miw.oneleft.chat.domain.model.ChatMessage;
import es.upm.miw.oneleft.chat.domain.model.PlanChat;
import es.upm.miw.oneleft.chat.domain.port.in.ChatUseCase;
import es.upm.miw.oneleft.chat.domain.port.out.ChatBroadcast;
import es.upm.miw.oneleft.chat.domain.port.out.ChatMessages;
import es.upm.miw.oneleft.chat.domain.port.out.PlanChats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The chat of each plan (HU-008). Who is in it comes from the plans service every time a chat is opened, so it is
 * always the group of the plan; while connected, departures and cancellations arrive as events.
 */
@Service
public class ChatService implements ChatUseCase {

    /** Messages per page of history: enough to fill a phone screen and scroll up a little. */
    public static final int HISTORY_PAGE = 50;

    private final PlanChats plans;
    private final ChatMessages messages;
    private final ChatBroadcast broadcast;
    private final Clock clock;
    private final Duration retention;

    public ChatService(PlanChats plans, ChatMessages messages, ChatBroadcast broadcast, Clock clock,
                       @Value("${oneleft.chat.retention}") Duration retention) {
        this.plans = plans;
        this.messages = messages;
        this.broadcast = broadcast;
        this.clock = clock;
        this.retention = retention;
    }

    @Override
    public PlanChat open(UUID planId, UUID userId) {
        return plans.find(planId).readableBy(userId);
    }

    @Override
    public ChatHistory history(UUID planId, UUID userId, Instant before) {
        var chat = open(planId, userId);
        var page = messages.latest(planId, Objects.requireNonNullElseGet(before, clock::instant), HISTORY_PAGE);
        return new ChatHistory(chat, chat.isOpen(clock.instant()), page.reversed());
    }

    @Override
    public ChatMessage send(PlanChat chat, UUID authorId, String authorName, String text) {
        var message = messages.save(ChatMessage.write(chat, authorId, authorName, text, clock));
        broadcast.send(message);
        return message;
    }

    @Override
    public int purge() {
        return messages.deleteEndedBefore(clock.instant().minus(retention));
    }
}

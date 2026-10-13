package es.upm.miw.oneleft.chat.application;

import es.upm.miw.oneleft.chat.domain.model.ChatException;
import es.upm.miw.oneleft.chat.domain.model.ChatMessage;
import es.upm.miw.oneleft.chat.domain.model.PlanChat;
import es.upm.miw.oneleft.chat.domain.port.out.ChatMessages;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-008: reading, writing and purging the chat of a plan. */
class ChatServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-12T18:00:00Z");
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUCIA = UUID.randomUUID();
    private static final PlanChat CHAT = new PlanChat(UUID.randomUUID(), Set.of(ANA, LUCIA), NOW.plusSeconds(3600),
            false);

    private final Messages messages = new Messages();
    private final List<ChatMessage> broadcast = new ArrayList<>();
    private final ChatService service = new ChatService(planId -> {
        if (!planId.equals(CHAT.planId())) {
            throw new ChatException(ChatException.Reason.PLAN_NOT_FOUND, "No plan");
        }
        return CHAT;
    }, messages, broadcast::add, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(24));

    @Test
    void aMessageIsSavedAndReachesEveryoneConnected() {
        var sent = service.send(CHAT, LUCIA, "Lucía", "On my way");

        assertThat(messages.saved).containsExactly(sent);
        assertThat(broadcast).containsExactly(sent);
        assertThat(sent.sentAt()).isEqualTo(NOW);
    }

    @Test
    void theHistoryComesOldestFirstAPageAtATimeAndSaysWhetherItIsOpen() {
        IntStream.range(0, 60).forEach(minute -> messages.save(new ChatMessage(UUID.randomUUID(), CHAT.planId(), ANA,
                "Ana", "Message " + minute, NOW.minusSeconds(3600 - minute * 60L), CHAT.endsAt())));

        var latest = service.history(CHAT.planId(), LUCIA, null);
        assertThat(latest.open()).isTrue();
        assertThat(latest.messages()).hasSize(ChatService.HISTORY_PAGE);
        assertThat(latest.messages().getFirst().text()).isEqualTo("Message 10");
        assertThat(latest.messages().getLast().text()).isEqualTo("Message 59");

        var older = service.history(CHAT.planId(), LUCIA, latest.messages().getFirst().sentAt());
        assertThat(older.messages()).extracting(ChatMessage::text).first().isEqualTo("Message 0");
        assertThat(older.messages()).hasSize(10);
    }

    @Test
    void onlyThePeopleOfThePlanReadIt() {
        assertThatThrownBy(() -> service.history(CHAT.planId(), UUID.randomUUID(), null))
                .isInstanceOfSatisfying(ChatException.class,
                        error -> assertThat(error.code()).isEqualTo("chat.notMember"));
        assertThatThrownBy(() -> service.open(UUID.randomUUID(), ANA))
                .isInstanceOfSatisfying(ChatException.class,
                        error -> assertThat(error.code()).isEqualTo("plan.notFound"));
        assertThat(service.open(CHAT.planId(), ANA)).isEqualTo(CHAT);
    }

    @Test
    void theMessagesAreDeletedADayAfterThePlanEnds() {
        service.purge();

        assertThat(messages.purgedBefore).isEqualTo(NOW.minus(Duration.ofHours(24)));
    }

    /** Messages in memory, like the database: newest first, before a moment and limited. */
    static final class Messages implements ChatMessages {

        private final List<ChatMessage> saved = new ArrayList<>();
        private Instant purgedBefore;

        @Override
        public ChatMessage save(ChatMessage message) {
            saved.add(message);
            return message;
        }

        @Override
        public List<ChatMessage> latest(UUID planId, Instant before, int limit) {
            return saved.stream().filter(message -> message.planId().equals(planId))
                    .filter(message -> message.sentAt().isBefore(before))
                    .sorted(Comparator.comparing(ChatMessage::sentAt).reversed()).limit(limit).toList();
        }

        @Override
        public int deleteEndedBefore(Instant moment) {
            purgedBefore = moment;
            return 0;
        }
    }
}

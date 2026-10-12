package es.upm.miw.oneleft.chat.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-008: who is in the chat of a plan, and until when they write. */
class PlanChatTest {

    private static final Instant NOW = Instant.parse("2026-10-12T18:00:00Z");
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private static final PlanChat CHAT = new PlanChat(UUID.randomUUID(), Set.of(ANA, LUCIA), NOW.plusSeconds(3600),
            false);

    private static void assertRejected(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ChatException.class, error -> assertThat(error.code()).isEqualTo(code));
    }

    @Test
    void onlyTheOrganizerAndTheParticipantsAreInIt() {
        assertThat(CHAT.readableBy(ANA)).isSameAs(CHAT);
        assertThat(CHAT.writableBy(LUCIA, NOW)).isSameAs(CHAT);
        assertRejected(() -> CHAT.readableBy(DIEGO), "chat.notMember");
        assertRejected(() -> CHAT.writableBy(DIEGO, NOW), "chat.notMember");
    }

    @Test
    void itIsReadOnlyOnceThePlanIsOverOrCancelled() {
        assertThat(CHAT.isOpen(NOW)).isTrue();
        assertThat(CHAT.isOpen(CHAT.endsAt())).isFalse();
        assertRejected(() -> CHAT.writableBy(ANA, CHAT.endsAt()), "chat.closed");
        assertThat(CHAT.readableBy(ANA)).isSameAs(CHAT);

        var cancelled = new PlanChat(CHAT.planId(), CHAT.members(), CHAT.endsAt(), true);
        assertThat(cancelled.isOpen(NOW)).isFalse();
        assertRejected(() -> cancelled.writableBy(ANA, NOW), "chat.closed");
    }

    @Test
    void aMessageHasItsTextTrimmedAndRemembersWhenThePlanEnds() {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);

        var message = ChatMessage.write(CHAT, LUCIA, " Lucía ", "  I'll bring the balls  ", clock);

        assertThat(message.text()).isEqualTo("I'll bring the balls");
        assertThat(message.authorName()).isEqualTo("Lucía");
        assertThat(message.sentAt()).isEqualTo(NOW);
        assertThat(message.planEndsAt()).isEqualTo(CHAT.endsAt());
        assertThat(message.planId()).isEqualTo(CHAT.planId());
    }

    @Test
    void aMessageHasFromOneToFiveHundredCharacters() {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);

        assertThat(ChatMessage.write(CHAT, ANA, "Ana", "x".repeat(500), clock).text()).hasSize(500);
        assertThatThrownBy(() -> ChatMessage.write(CHAT, ANA, "Ana", "   ", clock))
                .isInstanceOfSatisfying(ValidationException.class,
                        error -> assertThat(error.code()).isEqualTo("chat.text"));
        assertThatThrownBy(() -> ChatMessage.write(CHAT, ANA, "Ana", "x".repeat(501), clock))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ChatMessage.write(CHAT, ANA, "Ana", null, clock))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void aMissingOrLongNameIsShownAnyway() {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);

        assertThat(ChatMessage.write(CHAT, ANA, null, "Hi", clock).authorName()).isEqualTo("?");
        assertThat(ChatMessage.write(CHAT, ANA, "A".repeat(150), "Hi", clock).authorName()).hasSize(100);
        assertThat(ChatMessage.write(CHAT, ANA, "Ana", "Hi", clock.withZone(ZoneOffset.UTC)).sentAt())
                .isEqualTo(NOW);
        assertThat(Duration.between(NOW, CHAT.endsAt())).isEqualTo(Duration.ofHours(1));
    }
}

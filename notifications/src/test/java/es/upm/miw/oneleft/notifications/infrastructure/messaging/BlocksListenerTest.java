package es.upm.miw.oneleft.notifications.infrastructure.messaging;

import es.upm.miw.oneleft.notifications.domain.port.in.KeepBlocksUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-033: both events of the social service share a queue; the routing key says which one it is. */
class BlocksListenerTest {

    private final List<String> copied = new ArrayList<>();
    private final BlocksListener listener = new BlocksListener(new KeepBlocksUseCase() {
        @Override
        public void blocked(UUID blockerId, UUID blockedId, Instant at) {
            copied.add("blocked " + blockerId + ">" + blockedId + " " + at);
        }

        @Override
        public void unblocked(UUID blockerId, UUID blockedId) {
            copied.add("unblocked " + blockerId + ">" + blockedId);
        }
    });

    @Test
    void aBlockAndTheUnblockThatFollowsItAreCopiedInThatOrder() {
        var lucia = UUID.randomUUID();
        var ana = UUID.randomUUID();
        var at = Instant.parse("2026-10-24T18:00:00Z");

        listener.onBlock(new BlocksListener.Block(lucia, ana, at), RabbitConfig.USER_BLOCKED);
        listener.onBlock(new BlocksListener.Block(lucia, ana, at.plusSeconds(60)), RabbitConfig.USER_UNBLOCKED);

        assertThat(copied).containsExactly("blocked " + lucia + ">" + ana + " " + at,
                "unblocked " + lucia + ">" + ana);
    }
}

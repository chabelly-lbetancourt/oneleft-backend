package es.upm.miw.oneleft.chat.infrastructure.websocket;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Messages of one connection in the last few seconds: a chat is for talking, not for flooding it. The rate limit of the
 * gateway only sees the connection, not the messages that travel through it.
 */
final class MessageRate {

    static final int MAX_MESSAGES = 10;
    static final Duration WINDOW = Duration.ofSeconds(10);

    private final Deque<Instant> sent = new ArrayDeque<>();

    /** Records a message at {@code now} if it is within the limit. */
    synchronized boolean tryAcquire(Instant now) {
        while (!sent.isEmpty() && !sent.peekFirst().isAfter(now.minus(WINDOW))) {
            sent.pollFirst();
        }
        if (sent.size() >= MAX_MESSAGES) {
            return false;
        }
        sent.addLast(now);
        return true;
    }
}

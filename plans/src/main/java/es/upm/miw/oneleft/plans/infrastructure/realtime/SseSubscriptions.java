package es.upm.miw.oneleft.plans.infrastructure.realtime;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Clients connected through Server-Sent Events, each one with a key that decides what it receives (a search, a
 * user...). Subscriptions live in the memory of each replica; it works with several replicas because each one
 * receives every RabbitMQ event in its own anonymous queue.
 *
 * @param <K> what each subscription is interested in
 */
public abstract class SseSubscriptions<K> {

    /** The client reconnects when it expires; this frees forgotten connections. */
    static final Duration TIMEOUT = Duration.ofMinutes(30);
    static final String READY = "ready";

    private static final Logger log = LoggerFactory.getLogger(SseSubscriptions.class);

    private final Map<SseEmitter, K> subscriptions = new ConcurrentHashMap<>();

    protected SseSubscriptions(MeterRegistry meterRegistry, String gauge, String description) {
        Gauge.builder(gauge, subscriptions, Map::size).description(description).register(meterRegistry);
    }

    protected SseEmitter register(K key) {
        var emitter = new SseEmitter(TIMEOUT.toMillis());
        emitter.onCompletion(() -> subscriptions.remove(emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> subscriptions.remove(emitter));
        subscriptions.put(emitter, key);
        // First event: confirms the subscription and makes the headers reach the client right away
        send(emitter, SseEmitter.event().name(READY).data("ok"));
        return emitter;
    }

    /** Offers each subscription to the sender, which decides whether to send it something. */
    protected void forEach(BiConsumer<K, SseEmitter> sender) {
        subscriptions.forEach((emitter, key) -> sender.accept(key, emitter));
    }

    /** Heartbeat so that proxies and load balancers do not cut idle connections. */
    @Scheduled(fixedRateString = "${oneleft.realtime.heartbeat:PT20S}")
    public void heartbeat() {
        subscriptions.keySet().forEach(emitter -> send(emitter, SseEmitter.event().comment("ping")));
    }

    int size() {
        return subscriptions.size();
    }

    protected void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            // The client is gone: forget the subscription
            log.debug("Subscription closed: {}", exception.getMessage());
            subscriptions.remove(emitter);
        }
    }
}

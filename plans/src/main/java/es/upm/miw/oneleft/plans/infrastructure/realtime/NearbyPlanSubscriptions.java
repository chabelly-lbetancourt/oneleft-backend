package es.upm.miw.oneleft.plans.infrastructure.realtime;

import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.PlanPublished;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clients watching nearby plans, each one with its search. When a plan is published, only the clients it matches are
 * notified through Server-Sent Events.
 *
 * <p>Subscriptions live in the memory of each replica. It works with several replicas because each one receives
 * every RabbitMQ event in its own queue (see {@code PlanPublishedListener}).
 */
@Component
public class NearbyPlanSubscriptions {

    /** The client reconnects when it expires; this frees forgotten connections. */
    static final Duration TIMEOUT = Duration.ofMinutes(30);
    static final String READY = "ready";
    static final String PLAN_PUBLISHED = "plan-published";

    private static final Logger log = LoggerFactory.getLogger(NearbyPlanSubscriptions.class);

    private final Map<SseEmitter, NearbySearch> subscriptions = new ConcurrentHashMap<>();
    private final Clock clock;

    public NearbyPlanSubscriptions(Clock clock, MeterRegistry meterRegistry) {
        this.clock = clock;
        Gauge.builder("oneleft.plans.nearby.subscriptions", subscriptions, Map::size)
                .description("Clients connected to the real-time list of nearby plans")
                .register(meterRegistry);
    }

    public SseEmitter subscribe(NearbySearch search) {
        var emitter = new SseEmitter(TIMEOUT.toMillis());
        emitter.onCompletion(() -> subscriptions.remove(emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> subscriptions.remove(emitter));
        subscriptions.put(emitter, search);
        // First event: confirms the subscription and makes the headers reach the client right away
        send(emitter, SseEmitter.event().name(READY).data("ok"));
        return emitter;
    }

    public void dispatch(PlanPublished event) {
        var now = clock.instant();
        subscriptions.forEach((emitter, search) -> {
            if (search.matches(event, now)) {
                send(emitter, SseEmitter.event().name(PLAN_PUBLISHED).id(event.planId().toString())
                        .data(NearbyPlanEvent.of(event, search), MediaType.APPLICATION_JSON));
            }
        });
    }

    /** Heartbeat so that proxies and load balancers do not cut idle connections. */
    @Scheduled(fixedRateString = "${oneleft.realtime.heartbeat:PT20S}")
    public void heartbeat() {
        subscriptions.keySet().forEach(emitter -> send(emitter, SseEmitter.event().comment("ping")));
    }

    int size() {
        return subscriptions.size();
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            // The client is gone: forget the subscription
            log.debug("Subscription closed: {}", exception.getMessage());
            subscriptions.remove(emitter);
        }
    }
}

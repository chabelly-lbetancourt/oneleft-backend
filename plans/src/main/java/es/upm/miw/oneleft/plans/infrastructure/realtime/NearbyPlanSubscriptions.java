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
 * Clientes que están mirando planes cercanos, cada uno con su búsqueda. Cuando se publica un plan, se avisa por
 * Server-Sent Events solo a quienes les encaja.
 *
 * <p>Las suscripciones viven en la memoria de cada réplica. Con varias réplicas funciona porque cada una recibe
 * todos los eventos de RabbitMQ en su propia cola (ver {@code PlanPublishedListener}).
 */
@Component
public class NearbyPlanSubscriptions {

    /** El cliente vuelve a conectarse al caducar; así se liberan conexiones olvidadas. */
    static final Duration TIMEOUT = Duration.ofMinutes(30);
    static final String READY = "ready";
    static final String PLAN_PUBLISHED = "plan-published";

    private static final Logger log = LoggerFactory.getLogger(NearbyPlanSubscriptions.class);

    private final Map<SseEmitter, NearbySearch> subscriptions = new ConcurrentHashMap<>();
    private final Clock clock;

    public NearbyPlanSubscriptions(Clock clock, MeterRegistry meterRegistry) {
        this.clock = clock;
        Gauge.builder("oneleft.plans.nearby.subscriptions", subscriptions, Map::size)
                .description("Clientes conectados a la lista de planes cercanos en tiempo real")
                .register(meterRegistry);
    }

    public SseEmitter subscribe(NearbySearch search) {
        var emitter = new SseEmitter(TIMEOUT.toMillis());
        emitter.onCompletion(() -> subscriptions.remove(emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> subscriptions.remove(emitter));
        subscriptions.put(emitter, search);
        // Primer evento: confirma la suscripción y hace que las cabeceras lleguen ya al cliente
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

    /** Latido para que proxies y balanceadores no corten las conexiones sin tráfico. */
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
            // El cliente se ha ido: se olvida la suscripción
            log.debug("Suscripción cerrada: {}", exception.getMessage());
            subscriptions.remove(emitter);
        }
    }
}

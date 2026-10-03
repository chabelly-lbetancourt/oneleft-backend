package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.PlanReminder;
import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Output adapter: sends the notice to the browser's push service (RFC 8030), encrypted (RFC 8291) and signed (VAPID,
 * RFC 8292). The service worker of the web app shows it and opens the plan when it is tapped.
 */
@Component
class WebPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);
    /** The push service keeps the message until the browser is online, but never beyond the start of the plan. */
    private static final Duration MIN_TTL = Duration.ofMinutes(1);
    private static final Duration MAX_TTL = Duration.ofHours(12);

    private final Vapid vapid;
    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Clock clock;

    WebPushSender(WebPushProperties properties, RestClient.Builder http, Clock clock) {
        this.vapid = properties.configured()
                ? Vapid.of(properties.publicKey(), properties.privateKey(), properties.subject())
                : null;
        this.http = http.build();
        this.clock = clock;
        if (vapid == null) {
            log.warn("Web Push is off: VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY are not set");
        }
    }

    @Override
    public boolean enabled() {
        return vapid != null;
    }

    @Override
    public Result send(PushSubscription subscription, NearbyPlanNotice notice) {
        var language = subscription.language();
        return deliver(subscription, message(NoticeTexts.title(notice, language),
                NoticeTexts.body(notice, language, clock.getZone()), notice.planId(), "plan-" + notice.planId()),
                notice.startsAt());
    }

    @Override
    public Result send(PushSubscription subscription, PlanReminder reminder) {
        var language = subscription.language();
        // Its own tag: the reminder does not replace the notice of the same plan if it is still shown
        return deliver(subscription, message(NoticeTexts.title(reminder, language),
                NoticeTexts.body(reminder, language, clock.getZone()), reminder.planId(),
                "plan-" + reminder.planId() + "-reminder"), reminder.startsAt());
    }

    /** What the service worker shows: title, text, the plan to open and a tag so that a message is shown once. */
    private static Map<String, Object> message(String title, String body, UUID planId, String tag) {
        var message = new LinkedHashMap<String, Object>();
        message.put("title", title);
        message.put("body", body);
        message.put("url", "/plans/" + planId);
        message.put("tag", tag);
        return message;
    }

    /** The message is useless once the plan has started: it expires then, within the limits of the push services. */
    private Result deliver(PushSubscription subscription, Map<String, Object> message, Instant startsAt) {
        if (vapid == null) {
            return Result.FAILED;
        }
        var endpoint = URI.create(subscription.endpoint());
        var payload = json.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
        var body = WebPushEncryption.encrypt(payload, subscription.p256dh(), subscription.auth());
        var ttl = Duration.between(clock.instant(), startsAt);
        ttl = ttl.compareTo(MIN_TTL) < 0 ? MIN_TTL : ttl.compareTo(MAX_TTL) > 0 ? MAX_TTL : ttl;
        try {
            http.post().uri(endpoint)
                    .header("Authorization", vapid.authorization(endpoint, clock.instant()))
                    .header("Content-Encoding", "aes128gcm")
                    .header("TTL", Long.toString(ttl.toSeconds()))
                    .header("Urgency", "high")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return Result.SENT;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND) || e.getStatusCode().isSameCodeAs(HttpStatus.GONE)) {
                return Result.GONE;
            }
            log.warn("Web Push to {} failed: {} {}", endpoint.getHost(), e.getStatusCode(), e.getResponseBodyAsString());
            return Result.FAILED;
        } catch (RestClientException e) {
            log.warn("Web Push to {} failed: {}", endpoint.getHost(), e.getMessage());
            return Result.FAILED;
        }
    }
}

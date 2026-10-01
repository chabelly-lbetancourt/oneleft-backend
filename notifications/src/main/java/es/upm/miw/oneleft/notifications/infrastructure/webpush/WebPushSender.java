package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
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
import java.util.LinkedHashMap;

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
        if (vapid == null) {
            return Result.FAILED;
        }
        var endpoint = URI.create(subscription.endpoint());
        var body = WebPushEncryption.encrypt(payload(subscription, notice), subscription.p256dh(), subscription.auth());
        var ttl = Duration.between(clock.instant(), notice.startsAt());
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

    /** What the service worker shows: title, text, the plan to open and a tag so that a plan is shown once. */
    private byte[] payload(PushSubscription subscription, NearbyPlanNotice notice) {
        var message = new LinkedHashMap<String, Object>();
        message.put("title", NoticeTexts.title(notice, subscription.language()));
        message.put("body", NoticeTexts.body(notice, subscription.language(), clock.getZone()));
        message.put("url", "/plans/" + notice.planId());
        message.put("tag", "plan-" + notice.planId());
        return json.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
    }
}

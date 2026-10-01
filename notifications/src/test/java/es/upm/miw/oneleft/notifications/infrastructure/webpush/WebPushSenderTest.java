package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;
import es.upm.miw.oneleft.notifications.domain.model.PushSubscription;
import es.upm.miw.oneleft.notifications.domain.port.out.PushSender;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class WebPushSenderTest {

    private static final Instant NOW = Instant.parse("2026-11-16T16:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Europe/Madrid"));
    private static final String ENDPOINT = "https://push.example.org/send/abc";
    private static final UUID LUCIA = UUID.randomUUID();
    private static final NearbyPlanNotice NOTICE = new NearbyPlanNotice(LUCIA, UUID.randomUUID(), Activity.PADEL,
            "Pádel 2 contra 2, falta uno", "Pistas de la Albufera", NOW.plus(Duration.ofMinutes(80)), 1, 700);

    private final WebPushBrowser browser = new WebPushBrowser();
    private final RestClient.Builder http = RestClient.builder();
    private final MockRestServiceServer pushService = MockRestServiceServer.bindTo(http).build();

    private static WebPushProperties keys() {
        var server = EcKeys.generate();
        return new WebPushProperties(EcKeys.encode(EcKeys.uncompressed((ECPublicKey) server.getPublic())),
                EcKeys.encode(((ECPrivateKey) server.getPrivate()).getS().toByteArray()), "mailto:oneleft@example.org");
    }

    private PushSubscription subscription(String language) {
        return new PushSubscription(LUCIA, ENDPOINT, browser.p256dh(), browser.authSecret(), language);
    }

    @Test
    void sendsTheNoticeEncryptedForTheBrowserAndSigned() throws Exception {
        var sender = new WebPushSender(keys(), http, CLOCK);
        var body = new AtomicReference<byte[]>();
        pushService.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Encoding", "aes128gcm"))
                .andExpect(header("TTL", "4800"))
                .andExpect(header("Urgency", "high"))
                .andExpect(request -> assertThat(request.getHeaders().getFirst("Authorization")).startsWith("vapid t="))
                .andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsBytes()))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertThat(sender.enabled()).isTrue();
        assertThat(sender.send(subscription("es"), NOTICE)).isEqualTo(PushSender.Result.SENT);

        pushService.verify();
        var message = JsonMapper.builder().build().readValue(new String(browser.read(body.get()), StandardCharsets.UTF_8), Map.class);
        assertThat(message).containsEntry("title", "Plan cerca: Pádel 2 contra 2, falta uno")
                .containsEntry("body", "Pádel a las 18:20 · Pistas de la Albufera · a 0,7 km · Falta 1")
                .containsEntry("url", "/plans/" + NOTICE.planId())
                .containsEntry("tag", "plan-" + NOTICE.planId());
    }

    @Test
    void writesTheNoticeInTheLanguageOfTheSubscription() throws Exception {
        var sender = new WebPushSender(keys(), http, CLOCK);
        var body = new AtomicReference<byte[]>();
        pushService.expect(requestTo(ENDPOINT))
                .andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsBytes()))
                .andRespond(withStatus(HttpStatus.CREATED));

        sender.send(subscription("en"), new NearbyPlanNotice(LUCIA, NOTICE.planId(), Activity.BOARD_GAMES, "Catan",
                "Café La Partida", NOW.plus(Duration.ofHours(2)), 2, 1_200));

        var message = JsonMapper.builder().build().readValue(new String(browser.read(body.get()), StandardCharsets.UTF_8), Map.class);
        assertThat(message).containsEntry("title", "Plan nearby: Catan")
                .containsEntry("body", "Board games at 19:00 · Café La Partida · 1.2 km away · 2 spots left");
    }

    @Test
    void reportsTheSubscriptionsThatNoLongerExist() {
        var sender = new WebPushSender(keys(), http, CLOCK);
        pushService.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.GONE));
        pushService.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        pushService.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(sender.send(subscription("es"), NOTICE)).isEqualTo(PushSender.Result.GONE);
        assertThat(sender.send(subscription("es"), NOTICE)).isEqualTo(PushSender.Result.GONE);
        assertThat(sender.send(subscription("es"), NOTICE)).isEqualTo(PushSender.Result.FAILED);
    }

    @Test
    void theMessageExpiresWithinLimitsAroundTheStartOfThePlan() {
        var sender = new WebPushSender(keys(), http, CLOCK);
        pushService.expect(requestTo(ENDPOINT)).andExpect(header("TTL", "60")).andRespond(withStatus(HttpStatus.CREATED));
        pushService.expect(requestTo(ENDPOINT)).andExpect(header("TTL", "43200")).andRespond(withStatus(HttpStatus.CREATED));

        sender.send(subscription("es"), withStart(NOW.plusSeconds(10)));
        sender.send(subscription("es"), withStart(NOW.plus(Duration.ofDays(1))));

        pushService.verify();
    }

    @Test
    void withoutServerKeysWebPushIsOff() {
        var sender = new WebPushSender(new WebPushProperties("", "", "mailto:x@example.org"), http, CLOCK);

        assertThat(sender.enabled()).isFalse();
        assertThat(sender.send(subscription("es"), NOTICE)).isEqualTo(PushSender.Result.FAILED);
        assertThat(new WebPushProperties(null, null, null).configured()).isFalse();
    }

    private static NearbyPlanNotice withStart(Instant startsAt) {
        return new NearbyPlanNotice(LUCIA, NOTICE.planId(), Activity.PADEL, "Pádel", "Pistas", startsAt, 1, 700);
    }
}

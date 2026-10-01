package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VapidTest {

    @Test
    void signsATokenForTheOriginOfThePushService() throws Exception {
        var keys = EcKeys.generate();
        var vapid = new Vapid((ECPublicKey) keys.getPublic(), (ECPrivateKey) keys.getPrivate(), "mailto:oneleft@example.org");
        var now = Instant.parse("2026-11-16T10:00:00Z");

        var header = vapid.authorization(URI.create("https://fcm.googleapis.com/fcm/send/abc:def?x=1"), now);

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + vapid.publicKeyBase64Url());
        var token = header.substring("vapid t=".length(), header.indexOf(", k="));
        var parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        var mapper = JsonMapper.builder().build();
        assertThat(mapper.readValue(EcKeys.decode(parts[0]), Map.class)).containsEntry("alg", "ES256");
        var claims = mapper.readValue(EcKeys.decode(parts[1]), Map.class);
        assertThat(claims).containsEntry("aud", "https://fcm.googleapis.com").containsEntry("sub", "mailto:oneleft@example.org");
        assertThat(((Number) claims.get("exp")).longValue()).isEqualTo(now.plus(Vapid.VALIDITY).getEpochSecond());

        // Anyone with the public key can check it was signed with the private one
        var verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(keys.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(EcKeys.decode(parts[2]))).isTrue();
    }

    @Test
    void readsTheKeysFromTheirBase64UrlForm() {
        var keys = EcKeys.generate();
        var publicKey = EcKeys.encode(EcKeys.uncompressed((ECPublicKey) keys.getPublic()));
        var privateKey = EcKeys.encode(((ECPrivateKey) keys.getPrivate()).getS().toByteArray());

        var vapid = Vapid.of(publicKey, privateKey, "mailto:oneleft@example.org");

        assertThat(vapid.publicKeyBase64Url()).isEqualTo(publicKey);
        assertThat(vapid.privateKey().getS()).isEqualTo(((ECPrivateKey) keys.getPrivate()).getS());
    }
}

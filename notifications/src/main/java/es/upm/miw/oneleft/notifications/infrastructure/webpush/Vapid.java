package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;

/**
 * Voluntary Application Server Identification (RFC 8292): every push request carries a JWT signed with the server's
 * key, so the push service knows who sends it and only that server can use the subscriptions made with its public key.
 *
 * @param publicKey  the public key the browser subscribes with (the web app reads it from the API)
 * @param privateKey the key that signs the tokens, only in the server's configuration
 * @param subject    contact of the sender for the push service ({@code mailto:} or {@code https:})
 */
public record Vapid(ECPublicKey publicKey, ECPrivateKey privateKey, String subject) {

    /** Push services accept tokens valid for at most 24 hours. */
    static final Duration VALIDITY = Duration.ofHours(12);

    public static Vapid of(String publicKey, String privateKey, String subject) {
        return new Vapid(EcKeys.publicKey(EcKeys.decode(publicKey)), EcKeys.privateKey(EcKeys.decode(privateKey)), subject);
    }

    public String publicKeyBase64Url() {
        return EcKeys.encode(EcKeys.uncompressed(publicKey));
    }

    /** Value of the {@code Authorization} header for a request to the push service of the endpoint. */
    public String authorization(URI endpoint, Instant now) {
        var audience = endpoint.getScheme() + "://" + endpoint.getAuthority();
        var header = base64Url("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
        var claims = base64Url("{\"aud\":\"%s\",\"exp\":%d,\"sub\":\"%s\"}"
                .formatted(audience, now.plus(VALIDITY).getEpochSecond(), subject));
        var unsigned = header + "." + claims;
        try {
            // JWS wants the raw r || s signature (64 bytes), not the DER encoding Java uses by default
            var signer = Signature.getInstance("SHA256withECDSAinP1363Format");
            signer.initSign(privateKey);
            signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
            return "vapid t=" + unsigned + "." + EcKeys.encode(signer.sign()) + ", k=" + publicKeyBase64Url();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("VAPID signature failed", e);
        }
    }

    private static String base64Url(String json) {
        return EcKeys.encode(json.getBytes(StandardCharsets.UTF_8));
    }
}

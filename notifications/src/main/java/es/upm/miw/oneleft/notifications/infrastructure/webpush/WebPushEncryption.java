package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

/**
 * Message encryption for Web Push (RFC 8291) with the {@code aes128gcm} content coding (RFC 8188). Only the browser
 * that created the subscription can read the message: the push service in between (Google, Mozilla, Apple) cannot.
 * <p>
 * A fresh key pair and salt are used for every message; the shared secret comes from ECDH between that key pair and
 * the subscription's public key, mixed with the subscription's authentication secret.
 */
public final class WebPushEncryption {

    /** Record size of the only record: the whole message has to fit in it. */
    static final int RECORD_SIZE = 4096;
    private static final int SALT = 16;
    private static final int KEY = 16;
    private static final int NONCE = 12;
    private static final int TAG_BITS = 128;
    private static final byte LAST_RECORD = 0x02;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebPushEncryption() {
    }

    /** Encrypts for a subscription (its {@code p256dh} and {@code auth} keys, base64url). */
    public static byte[] encrypt(byte[] plaintext, String p256dh, String auth) {
        var salt = new byte[SALT];
        RANDOM.nextBytes(salt);
        return encrypt(plaintext, EcKeys.publicKey(EcKeys.decode(p256dh)), EcKeys.decode(auth), EcKeys.generate(), salt);
    }

    /** Deterministic form (given server keys and salt), used by the tests with the example of RFC 8291. */
    static byte[] encrypt(byte[] plaintext, ECPublicKey uaPublic, byte[] authSecret, KeyPair serverKeys, byte[] salt) {
        // A single record holds the plaintext, its delimiter and the authentication tag
        if (plaintext.length + 1 + TAG_BITS / 8 > RECORD_SIZE) {
            throw new IllegalArgumentException("Push message too long");
        }
        try {
            var agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(serverKeys.getPrivate());
            agreement.doPhase(uaPublic, true);
            var ecdhSecret = agreement.generateSecret();

            var uaPoint = EcKeys.uncompressed(uaPublic);
            var asPoint = EcKeys.uncompressed((ECPublicKey) serverKeys.getPublic());
            // RFC 8291, section 3.4: combine the ECDH secret with the authentication secret
            var keyInfo = concat(ascii("WebPush: info\0"), uaPoint, asPoint);
            var ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);
            // RFC 8188, section 2.2: content encryption key and nonce
            var cek = hkdf(salt, ikm, ascii("Content-Encoding: aes128gcm\0"), KEY);
            var nonce = hkdf(salt, ikm, ascii("Content-Encoding: nonce\0"), NONCE);

            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            var ciphertext = cipher.doFinal(concat(plaintext, new byte[]{LAST_RECORD}));

            // Header: salt, record size, length and value of the key id (the server's public key)
            var header = ByteBuffer.allocate(SALT + 4 + 1 + asPoint.length)
                    .put(salt).putInt(RECORD_SIZE).put((byte) asPoint.length).put(asPoint).array();
            return concat(header, ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Web Push encryption failed", e);
        }
    }

    /** HKDF with SHA-256 (RFC 5869) for outputs of at most one block, the only ones Web Push needs. */
    static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        var prk = hmac(salt, ikm);
        return Arrays.copyOf(hmac(prk, concat(info, new byte[]{0x01})), length);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] concat(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (var part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}

package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

/**
 * A browser subscribed to Web Push, for the tests: it reads messages as RFC 8291 describes from the receiving side,
 * independently of {@link WebPushEncryption}.
 */
final class WebPushBrowser {

    final KeyPair keys = EcKeys.generate();
    final byte[] auth = new byte[16];

    WebPushBrowser() {
        new java.security.SecureRandom().nextBytes(auth);
    }

    String p256dh() {
        return EcKeys.encode(EcKeys.uncompressed((ECPublicKey) keys.getPublic()));
    }

    String authSecret() {
        return EcKeys.encode(auth);
    }

    byte[] read(byte[] message) throws Exception {
        return decrypt(message, keys, auth);
    }

    static byte[] decrypt(byte[] message, KeyPair browser, byte[] auth) throws Exception {
        var buffer = ByteBuffer.wrap(message);
        var salt = new byte[16];
        buffer.get(salt);
        buffer.getInt();
        var serverPoint = new byte[buffer.get()];
        buffer.get(serverPoint);
        var ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        var agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(browser.getPrivate());
        agreement.doPhase(EcKeys.publicKey(serverPoint), true);
        var keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII),
                EcKeys.uncompressed((ECPublicKey) browser.getPublic()), serverPoint);
        var ikm = WebPushEncryption.hkdf(auth, agreement.generateSecret(), keyInfo, 32);
        var cek = WebPushEncryption.hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        var nonce = WebPushEncryption.hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        var padded = cipher.doFinal(ciphertext);
        if (padded[padded.length - 1] != 0x02) {
            throw new IllegalStateException("Missing last record delimiter");
        }
        return Arrays.copyOf(padded, padded.length - 1);
    }

    static byte[] concat(byte[]... parts) {
        var out = new java.io.ByteArrayOutputStream();
        for (var part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}

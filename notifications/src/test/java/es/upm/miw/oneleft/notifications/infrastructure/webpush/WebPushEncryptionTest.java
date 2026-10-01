package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebPushEncryptionTest {

    /** The example of RFC 8291, section 5 and appendix A. */
    private static final String PLAINTEXT = "V2hlbiBJIGdyb3cgdXAsIEkgd2FudCB0byBiZSBhIHdhdGVybWVsb24";
    private static final String AS_PUBLIC =
            "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String AS_PRIVATE = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
    private static final String UA_PUBLIC =
            "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String UA_PRIVATE = "q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94";
    private static final String SALT = "DGv6ra1nlYgDCS1FRnbzlw";
    private static final String AUTH = "BTBZMqHH6r4Tts7J_aSIgg";
    private static final String HEADER =
            "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String CIPHERTEXT =
            "8pfeW0KbunFT06SuDKoJH9Ql87S1QUrdirN6GcG7sFz1y1sqLgVi1VhjVkHsUoEsbI_0LpXMuGvnzQ";

    @Test
    void encryptsTheExampleOfRfc8291ByteForByte() {
        var serverKeys = new KeyPair(EcKeys.publicKey(EcKeys.decode(AS_PUBLIC)), EcKeys.privateKey(EcKeys.decode(AS_PRIVATE)));

        var message = WebPushEncryption.encrypt(EcKeys.decode(PLAINTEXT), EcKeys.publicKey(EcKeys.decode(UA_PUBLIC)),
                EcKeys.decode(AUTH), serverKeys, EcKeys.decode(SALT));

        assertThat(EcKeys.encode(message)).isEqualTo(EcKeys.encode(concat(EcKeys.decode(HEADER), EcKeys.decode(CIPHERTEXT))));
    }

    @Test
    void theBrowserOfTheSubscriptionCanReadTheMessage() throws Exception {
        for (int i = 0; i < 200; i++) {
            var browser = EcKeys.generate();
            var auth = new byte[16];
            auth[0] = (byte) i;
            var text = "{\"title\":\"Pádel en Vallecas\",\"n\":" + i + "}";

            var message = WebPushEncryption.encrypt(text.getBytes(StandardCharsets.UTF_8),
                    EcKeys.encode(EcKeys.uncompressed((ECPublicKey) browser.getPublic())), EcKeys.encode(auth));

            assertThat(new String(decrypt(message, browser, auth), StandardCharsets.UTF_8)).isEqualTo(text);
        }
    }

    @Test
    void theExampleIsReadableWithTheKeysOfTheUserAgent() throws Exception {
        var browser = new KeyPair(EcKeys.publicKey(EcKeys.decode(UA_PUBLIC)), EcKeys.privateKey(EcKeys.decode(UA_PRIVATE)));
        var message = concat(EcKeys.decode(HEADER), EcKeys.decode(CIPHERTEXT));

        assertThat(decrypt(message, browser, EcKeys.decode(AUTH))).isEqualTo(EcKeys.decode(PLAINTEXT));
    }

    @Test
    void aMessageMustFitInASingleRecord() {
        var browser = EcKeys.generate();
        var tooLong = new byte[WebPushEncryption.RECORD_SIZE];

        assertThatThrownBy(() -> WebPushEncryption.encrypt(tooLong,
                EcKeys.encode(EcKeys.uncompressed((ECPublicKey) browser.getPublic())), EcKeys.encode(new byte[16])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicKeysAreUncompressedP256Points() {
        assertThatThrownBy(() -> EcKeys.publicKey(new byte[33])).isInstanceOf(IllegalArgumentException.class);
        var keys = EcKeys.generate();
        var point = EcKeys.uncompressed((ECPublicKey) keys.getPublic());
        assertThat(point).hasSize(65).startsWith((byte) 0x04);
        assertThat(EcKeys.publicKey(point)).isEqualTo(keys.getPublic());
    }

    private static byte[] decrypt(byte[] message, KeyPair browser, byte[] auth) throws Exception {
        return WebPushBrowser.decrypt(message, browser, auth);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        return WebPushBrowser.concat(a, b);
    }
}

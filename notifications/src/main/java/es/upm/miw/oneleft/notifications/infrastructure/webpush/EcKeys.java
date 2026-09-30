package es.upm.miw.oneleft.notifications.infrastructure.webpush;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * P-256 keys in the formats Web Push uses: public keys as uncompressed points (65 bytes: 0x04, x, y) and private keys
 * as the raw 32-byte scalar, both in base64url without padding.
 */
public final class EcKeys {

    private static final int COORDINATE = 32;
    private static final ECParameterSpec P256 = p256();

    private EcKeys() {
    }

    public static KeyPair generate() {
        try {
            var generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is not available", e);
        }
    }

    public static ECPublicKey publicKey(byte[] uncompressed) {
        if (uncompressed.length != 1 + 2 * COORDINATE || uncompressed[0] != 0x04) {
            throw new IllegalArgumentException("Not an uncompressed P-256 point");
        }
        var x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 1 + COORDINATE));
        var y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1 + COORDINATE, uncompressed.length));
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid P-256 public key", e);
        }
    }

    public static ECPrivateKey privateKey(byte[] scalar) {
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC")
                    .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, scalar), P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid P-256 private key", e);
        }
    }

    public static byte[] uncompressed(ECPublicKey key) {
        var point = new byte[1 + 2 * COORDINATE];
        point[0] = 0x04;
        copyUnsigned(key.getW().getAffineX(), point, 1);
        copyUnsigned(key.getW().getAffineY(), point, 1 + COORDINATE);
        return point;
    }

    public static byte[] decode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url.strip());
    }

    public static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** A coordinate as exactly 32 bytes: BigInteger may add a sign byte or drop leading zeros. */
    private static void copyUnsigned(BigInteger value, byte[] target, int offset) {
        var bytes = value.toByteArray();
        var length = Math.min(bytes.length, COORDINATE);
        System.arraycopy(bytes, bytes.length - length, target, offset + COORDINATE - length, length);
    }

    private static ECParameterSpec p256() {
        try {
            var parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is not available", e);
        }
    }
}

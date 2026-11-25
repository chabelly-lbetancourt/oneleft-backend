package es.upm.miw.oneleft.plans.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * The secret of a link for a trusted contact (HU-034): 256 random bits from a cryptographic generator, so it cannot
 * be guessed or enumerated. Whoever has it sees the plan, so it is only given once, when the link is created, and
 * only its SHA-256 is stored: a fast hash is enough for a secret with this much entropy.
 */
public final class TrustedLinkToken {

    private static final int BYTES = 32;
    /** 32 bytes in URL-safe Base64 without padding */
    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private TrustedLinkToken() {
    }

    public static String random() {
        var bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Whether the text can be a token at all: anything else is not even looked up. */
    public static boolean isWellFormed(String token) {
        return token != null && FORMAT.matcher(token).matches();
    }

    /** What is stored and looked up instead of the token. */
    public static String hash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Every Java platform has SHA-256", impossible);
        }
    }
}

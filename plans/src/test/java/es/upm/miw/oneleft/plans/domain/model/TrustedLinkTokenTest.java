package es.upm.miw.oneleft.plans.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-034: the secret of a link cannot be guessed, and only its hash is kept. */
class TrustedLinkTokenTest {

    @Test
    void everyTokenIsDifferentUrlSafeAndCarries256Bits() {
        var tokens = new HashSet<String>();
        for (var i = 0; i < 1000; i++) {
            tokens.add(TrustedLinkToken.random());
        }

        assertThat(tokens).hasSize(1000).allSatisfy(token -> {
            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
            assertThat(TrustedLinkToken.isWellFormed(token)).isTrue();
        });
    }

    @Test
    void theHashIsTheSha256OfTheTokenInHexAndNeverTheToken() {
        // A known SHA-256: the one of «abc»
        assertThat(TrustedLinkToken.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");

        var token = TrustedLinkToken.random();
        assertThat(TrustedLinkToken.hash(token)).hasSize(64).isEqualTo(TrustedLinkToken.hash(token))
                .doesNotContain(token);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "has space in it, forty-three characters ok!",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "../../../../../../../../../../../../../etc/"})
    void whatCannotBeATokenIsNotWellFormed(String text) {
        assertThat(TrustedLinkToken.isWellFormed(text)).isFalse();
    }
}

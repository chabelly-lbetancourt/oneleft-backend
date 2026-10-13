package es.upm.miw.oneleft.plans.infrastructure.realtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Notice for someone: they earned a badge (HU-044). */
@Schema(description = "A badge earned")
public record BadgeEarnedNotice(@Schema(example = "ALWAYS_PUNCTUAL") String badge, Instant earnedAt) {

    public static BadgeEarnedNotice of(Message message) {
        return new BadgeEarnedNotice(message.badge(), message.earnedAt());
    }

    /** The notice as the reputation service sends it, with the person it is for. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(UUID userId, String badge, Instant earnedAt) {
    }
}

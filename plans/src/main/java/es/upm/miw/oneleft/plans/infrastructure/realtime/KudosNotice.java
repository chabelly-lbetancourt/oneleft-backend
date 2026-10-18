package es.upm.miw.oneleft.plans.infrastructure.realtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Notice for someone: a person of a plan gave them a kudos (HU-060). */
@Schema(description = "A kudos received")
public record KudosNotice(@Schema(example = "Ana") String giverName, @Schema(example = "GOOD_VIBES") String kind,
                          UUID planId, Instant givenAt) {

    public static KudosNotice of(Message message) {
        return new KudosNotice(message.giverName(), message.kind(), message.planId(), message.givenAt());
    }

    /** The notice as the reputation service sends it, with the person it is for. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(UUID userId, String giverName, String kind, UUID planId, Instant givenAt) {
    }
}

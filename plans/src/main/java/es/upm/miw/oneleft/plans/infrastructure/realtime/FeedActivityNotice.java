package es.upm.miw.oneleft.plans.infrastructure.realtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Notice for someone: a person they follow did something new (HU-051), for their feed. */
@Schema(description = "New activity of someone I follow")
public record FeedActivityNotice(UUID actorId, @Schema(example = "Ana") String actorName,
                                 @Schema(example = "JOINED") String type, UUID planId,
                                 @Schema(example = "Pádel 2 contra 2") String planTitle, Instant occurredAt) {

    public static FeedActivityNotice of(Message message) {
        return new FeedActivityNotice(message.actorId(), message.actorName(), message.type(), message.planId(),
                message.planTitle(), message.occurredAt());
    }

    /** The notice as the social service sends it, with the follower it is for. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(UUID userId, UUID actorId, String actorName, String type, UUID planId, String planTitle,
                          Instant occurredAt) {
    }
}

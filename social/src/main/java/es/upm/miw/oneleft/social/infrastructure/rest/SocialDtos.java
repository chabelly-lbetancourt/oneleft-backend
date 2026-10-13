package es.upm.miw.oneleft.social.infrastructure.rest;

import es.upm.miw.oneleft.social.domain.model.Connection;
import es.upm.miw.oneleft.social.domain.model.FollowStatus;
import es.upm.miw.oneleft.social.domain.model.PageResult;
import es.upm.miw.oneleft.social.domain.model.PersonView;
import es.upm.miw.oneleft.social.domain.model.Relation;
import es.upm.miw.oneleft.social.domain.model.SocialAccount;
import es.upm.miw.oneleft.social.domain.model.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SocialDtos {

    private SocialDtos() {
    }

    @Schema(description = "My profile in the social service and my counts")
    public record AccountResponse(UUID userId, @Schema(example = "Lucía") String name, Visibility visibility,
                                  long followers, long following,
                                  @Schema(description = "Requests waiting for my answer") long requests,
                                  @Schema(description = "My requests waiting for an answer") long sentRequests) {

        static AccountResponse of(SocialAccount account) {
            var counts = account.counts();
            return new AccountResponse(account.person().userId(), account.person().name(),
                    account.person().visibility(), counts.followers(), counts.following(), counts.requests(),
                    counts.sentRequests());
        }
    }

    @Schema(description = "Public or private profile")
    public record VisibilityRequest(@NotNull Visibility visibility) {
    }

    @Schema(description = "Someone as I see them")
    public record PersonResponse(UUID userId, @Schema(example = "Ana") String name, Visibility visibility,
                                 long followers, long following,
                                 @Schema(description = "NONE, REQUESTED (waiting for them), FOLLOWING or SELF")
                                 Relation relation,
                                 @Schema(description = "They follow me") boolean followsMe) {

        static PersonResponse of(PersonView view) {
            return new PersonResponse(view.person().userId(), view.person().name(), view.person().visibility(),
                    view.followers(), view.following(), view.relation(), view.followsMe());
        }
    }

    @Schema(description = "Someone in one of my lists")
    public record ConnectionResponse(UUID userId, @Schema(example = "Ana") String name,
                                     @Schema(description = "ACCEPTED, or PENDING for a request") FollowStatus status,
                                     @Schema(description = "Since the follow was accepted, or the request was sent")
                                     Instant since) {

        static ConnectionResponse of(Connection connection) {
            return new ConnectionResponse(connection.userId(), connection.name(), connection.status(),
                    connection.since());
        }
    }

    @Schema(description = "A page of a list, the latest first")
    public record PageResponse(List<ConnectionResponse> items, int page, int size, long total) {

        static PageResponse of(PageResult<Connection> result) {
            return new PageResponse(result.items().stream().map(ConnectionResponse::of).toList(), result.page(),
                    result.size(), result.total());
        }
    }
}

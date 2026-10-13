package es.upm.miw.oneleft.chat.infrastructure.plans;

import es.upm.miw.oneleft.chat.domain.model.ChatException;
import es.upm.miw.oneleft.chat.domain.model.PlanChat;
import es.upm.miw.oneleft.chat.domain.port.out.PlanChats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Output adapter: the group of a plan from {@code GET /api/v1/plans/{id}} of the plans service, which is always up to
 * date, also for the plans published before the chat existed. The request carries the token of whoever opens the
 * chat: the plans service sees the same as they do.
 */
@Component
public class RestPlanChats implements PlanChats {

    private static final Logger log = LoggerFactory.getLogger(RestPlanChats.class);

    private final RestClient plans;

    public RestPlanChats(RestClient plansService) {
        this.plans = plansService;
    }

    private static String currentToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            return authentication.getToken().getTokenValue();
        }
        throw new IllegalStateException("The chat is only opened with a Keycloak token");
    }

    @Override
    public PlanChat find(UUID planId) {
        try {
            var plan = plans.get().uri("/api/v1/plans/{id}", planId)
                    .headers(headers -> headers.setBearerAuth(currentToken()))
                    .retrieve()
                    .body(PlanView.class);
            if (plan == null) {
                throw new RestClientException("Empty answer");
            }
            return plan.toChat();
        } catch (HttpClientErrorException.NotFound error) {
            throw new ChatException(ChatException.Reason.PLAN_NOT_FOUND, "The plan does not exist");
        } catch (RestClientException error) {
            log.warn("The plans service did not say who is in plan {}: {}", planId, error.toString());
            throw new ChatException(ChatException.Reason.UNAVAILABLE, "The chat is not available right now");
        }
    }

    /** The part of the plan that the chat needs. */
    record PlanView(UUID id, UUID organizerId, List<Participant> participants, String status, Instant endsAt) {

        PlanChat toChat() {
            var members = new HashSet<UUID>();
            members.add(organizerId);
            if (participants != null) {
                participants.forEach(participant -> members.add(participant.userId()));
            }
            return new PlanChat(id, members, endsAt, "CANCELLED".equals(status));
        }
    }

    record Participant(UUID userId) {
    }
}

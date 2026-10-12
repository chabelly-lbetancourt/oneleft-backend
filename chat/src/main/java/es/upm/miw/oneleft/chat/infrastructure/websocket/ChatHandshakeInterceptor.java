package es.upm.miw.oneleft.chat.infrastructure.websocket;

import es.upm.miw.oneleft.chat.domain.model.ChatException;
import es.upm.miw.oneleft.chat.domain.port.in.ChatUseCase;
import es.upm.miw.oneleft.chat.infrastructure.config.KeycloakJwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Before the connection is accepted, the plans service says whether whoever connects is in the plan: otherwise the
 * handshake ends with 403 (not in the plan), 404 (no such plan) or 503 (the plans service did not answer).
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final ChatUseCase chat;

    public ChatHandshakeInterceptor(ChatUseCase chat) {
        this.chat = chat;
    }

    private static Optional<UUID> planId(ServerHttpRequest request) {
        try {
            return Optional.ofNullable(UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams()
                    .getFirst("planId")).map(UUID::fromString);
        } catch (IllegalArgumentException error) {
            return Optional.empty();
        }
    }

    private static HttpStatus status(ChatException.Reason reason) {
        return switch (reason) {
            case NOT_MEMBER -> HttpStatus.FORBIDDEN;
            case PLAN_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CLOSED -> HttpStatus.CONFLICT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        var planId = planId(request);
        if (planId.isEmpty() || !(request.getPrincipal() instanceof JwtAuthenticationToken authentication)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        var jwt = authentication.getToken();
        try {
            var userId = KeycloakJwt.userId(jwt);
            ChatAttributes.put(attributes, chat.open(planId.get(), userId), userId, KeycloakJwt.displayName(jwt));
            return true;
        } catch (ChatException error) {
            response.setStatusCode(status(error.reason()));
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                               Exception exception) {
        // Nothing to do: the handler takes over the connection
    }
}

package es.upm.miw.oneleft.gateway.chat;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Keeps the token already validated by Spring Security for the connection to the chat service. The plan comes in the
 * query of the URL and is checked by the chat service.
 */
class ChatRelayHandshake implements HandshakeInterceptor {

    static final String TOKEN = "oneleft.token";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        if (request.getPrincipal() instanceof JwtAuthenticationToken authentication) {
            attributes.put(TOKEN, authentication.getToken().getTokenValue());
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                               Exception exception) {
        // Nothing to do: the relay opens the connection to the chat service
    }
}

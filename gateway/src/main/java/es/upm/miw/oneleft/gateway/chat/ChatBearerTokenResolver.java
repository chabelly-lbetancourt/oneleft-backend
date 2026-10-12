package es.upm.miw.oneleft.gateway.chat;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Where the token of a request is. Browsers cannot add an {@code Authorization} header to a WebSocket, so the app sends
 * it as the second WebSocket subprotocol ({@code Sec-WebSocket-Protocol: oneleft.bearer, <token>}): unlike a query
 * parameter, it never ends up in a URL or a log. The rest of the API uses the usual header.
 */
public class ChatBearerTokenResolver implements BearerTokenResolver {

    static final String PROTOCOL = "oneleft.bearer";
    static final String PROTOCOL_HEADER = "Sec-WebSocket-Protocol";

    private final BearerTokenResolver standard = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith(ChatRelayConfig.PATH)) {
            return standard.resolve(request);
        }
        List<String> protocols = Collections.list(request.getHeaders(PROTOCOL_HEADER)).stream()
                .flatMap(header -> Arrays.stream(header.split(",")))
                .map(String::strip)
                .toList();
        var index = protocols.indexOf(PROTOCOL);
        return index >= 0 && index + 1 < protocols.size() ? protocols.get(index + 1) : null;
    }
}

package es.upm.miw.oneleft.gateway.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/** {@code /ws/chat?planId=}: the chat of a plan, for the web app and the Android app (same origins as CORS). */
@Configuration
@EnableWebSocket
public class ChatRelayConfig implements WebSocketConfigurer {

    public static final String PATH = "/ws/chat";

    private final ChatRelayHandler relay;
    private final List<String> allowedOrigins;

    public ChatRelayConfig(ChatRelayHandler relay,
                           @Value("${oneleft.cors.allowed-origins}") List<String> allowedOrigins) {
        this.relay = relay;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(relay, PATH)
                .addInterceptors(new ChatRelayHandshake())
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}

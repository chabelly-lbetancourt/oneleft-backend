package es.upm.miw.oneleft.chat.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** The chat WebSocket. Only the gateway connects to it, server to server, with the token of the person. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    public static final String PATH = "/ws/chat";

    private final ChatWebSocketHandler handler;
    private final ChatHandshakeInterceptor handshake;

    public WebSocketConfig(ChatWebSocketHandler handler, ChatHandshakeInterceptor handshake) {
        this.handler = handler;
        this.handshake = handshake;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, PATH).addInterceptors(handshake);
    }
}

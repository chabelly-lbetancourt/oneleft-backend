package es.upm.miw.oneleft.gateway.chat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-008: the token of the chat WebSocket travels as a subprotocol, the rest of the API in the usual header. */
class ChatBearerTokenResolverTest {

    private final ChatBearerTokenResolver resolver = new ChatBearerTokenResolver();

    private static MockHttpServletRequest request(String path) {
        var request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }

    @Test
    void theChatWebSocketTakesTheTokenThatFollowsItsSubprotocol() {
        var request = request("/ws/chat");
        request.addHeader("Sec-WebSocket-Protocol", "oneleft.bearer, eyJhbGciOi.eyJzdWIiOi.c2lnbmF0dXJl");

        assertThat(resolver.resolve(request)).isEqualTo("eyJhbGciOi.eyJzdWIiOi.c2lnbmF0dXJl");
    }

    @Test
    void withoutTheSubprotocolOrItsTokenThereIsNoToken() {
        var request = request("/ws/chat");
        assertThat(resolver.resolve(request)).isNull();

        request.addHeader("Sec-WebSocket-Protocol", "oneleft.bearer");
        assertThat(resolver.resolve(request)).isNull();

        // The usual header is not taken on the WebSocket: browsers cannot send it
        var withHeader = request("/ws/chat");
        withHeader.addHeader("Authorization", "Bearer abc");
        assertThat(resolver.resolve(withHeader)).isNull();
    }

    @Test
    void theRestOfTheApiUsesTheAuthorizationHeader() {
        var request = request("/api/v1/chats/abc/messages");
        request.addHeader("Authorization", "Bearer abc.def.ghi");
        request.addHeader("Sec-WebSocket-Protocol", "oneleft.bearer, other");

        assertThat(resolver.resolve(request)).isEqualTo("abc.def.ghi");
    }
}

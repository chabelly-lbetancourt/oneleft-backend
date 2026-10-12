package es.upm.miw.oneleft.chat.infrastructure.rest;

import es.upm.miw.oneleft.chat.domain.model.ChatHistory;
import es.upm.miw.oneleft.chat.domain.model.ChatMessage;
import es.upm.miw.oneleft.chat.domain.port.in.ChatUseCase;
import es.upm.miw.oneleft.chat.infrastructure.config.KeycloakJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ChatController.CHATS)
@Tag(name = "Chat", description = "The chat of each plan, for its organizer and participants (HU-008)")
public class ChatController {

    public static final String CHATS = "/api/v1/chats";
    public static final String MESSAGES = "/{planId}/messages";

    private final ChatUseCase chat;

    public ChatController(ChatUseCase chat) {
        this.chat = chat;
    }

    @GetMapping(MESSAGES)
    @Operation(summary = "History of the chat of a plan (HU-008)",
            description = "Up to 50 messages, oldest first, and whether the chat can still be written in. New "
                    + "messages arrive through the WebSocket /ws/chat?planId= of the gateway.")
    @ApiResponse(responseCode = "200", description = "History")
    @ApiResponse(responseCode = "403", description = "chat.notMember", content = @Content)
    @ApiResponse(responseCode = "404", description = "plan.notFound", content = @Content)
    @ApiResponse(responseCode = "503", description = "chat.unavailable", content = @Content)
    public ChatHistoryResponse history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId,
                                       @Parameter(description = "Only messages sent before this moment (older page)")
                                       @RequestParam(required = false) Instant before) {
        return ChatHistoryResponse.of(chat.history(planId, KeycloakJwt.userId(jwt), before));
    }

    @Schema(description = "A page of the chat of a plan")
    public record ChatHistoryResponse(UUID planId,
                                      @Schema(description = "False once the plan is over or cancelled") boolean open,
                                      @Schema(description = "When the plan ends and the chat closes") Instant endsAt,
                                      List<ChatMessageResponse> messages) {

        static ChatHistoryResponse of(ChatHistory history) {
            return new ChatHistoryResponse(history.chat().planId(), history.open(), history.chat().endsAt(),
                    history.messages().stream().map(ChatMessageResponse::of).toList());
        }
    }

    @Schema(description = "A message of the chat")
    public record ChatMessageResponse(UUID id, UUID authorId, @Schema(example = "Lucía") String authorName,
                                      @Schema(example = "I'll bring the balls") String text, Instant sentAt) {

        static ChatMessageResponse of(ChatMessage message) {
            return new ChatMessageResponse(message.id(), message.authorId(), message.authorName(), message.text(),
                    message.sentAt());
        }
    }
}

package es.upm.miw.oneleft.chat.infrastructure.persistence;

import es.upm.miw.oneleft.chat.domain.model.ChatMessage;
import es.upm.miw.oneleft.chat.domain.port.out.ChatMessages;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
class JpaChatMessages implements ChatMessages {

    private final SpringDataChatMessageRepository repository;

    JpaChatMessages(SpringDataChatMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ChatMessage save(ChatMessage message) {
        return repository.save(new ChatMessageEntity(message)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> latest(UUID planId, Instant before, int limit) {
        return repository.findByPlanIdAndSentAtBeforeOrderBySentAtDesc(planId, before, Limit.of(limit)).stream()
                .map(ChatMessageEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public int deleteEndedBefore(Instant moment) {
        return repository.deleteEndedBefore(moment);
    }
}

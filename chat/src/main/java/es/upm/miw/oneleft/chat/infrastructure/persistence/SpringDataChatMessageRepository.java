package es.upm.miw.oneleft.chat.infrastructure.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    List<ChatMessageEntity> findByPlanIdAndSentAtBeforeOrderBySentAtDesc(UUID planId, Instant before, Limit limit);

    @Modifying
    @Query("delete from ChatMessageEntity m where m.planEndsAt < :moment")
    int deleteEndedBefore(@Param("moment") Instant moment);
}

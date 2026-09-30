package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataPreferencesRepository extends JpaRepository<PreferencesEntity, UUID> {

    @Query("select p from PreferencesEntity p where p.enabled = true "
            + "and (p.activities is empty or :activity member of p.activities)")
    List<PreferencesEntity> findEnabledFor(@Param("activity") Activity activity);
}

interface SpringDataPushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, String> {

    List<PushSubscriptionEntity> findByUserId(UUID userId);

    @Modifying
    @Query("delete from PushSubscriptionEntity s where s.userId = :userId and s.endpoint = :endpoint")
    void deleteByUserAndEndpoint(@Param("userId") UUID userId, @Param("endpoint") String endpoint);
}

interface SpringDataSentNoticeRepository extends JpaRepository<SentNoticeEntity, SentNoticeEntity.Key> {

    @Query("select count(n) from SentNoticeEntity n where n.id.userId = :userId and n.sentAt >= :since")
    long countSince(@Param("userId") UUID userId, @Param("since") Instant since);
}

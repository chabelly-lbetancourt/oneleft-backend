package es.upm.miw.oneleft.notifications.infrastructure.persistence;

import es.upm.miw.oneleft.notifications.domain.port.out.NoticeLog;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
class JpaNoticeLog implements NoticeLog {

    private final SpringDataSentNoticeRepository repository;

    JpaNoticeLog(SpringDataSentNoticeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean record(UUID userId, UUID planId, Instant at) {
        // Each plan is handled by a single replica (shared queue), so checking first is enough
        if (repository.existsById(new SentNoticeEntity.Key(userId, planId))) {
            return false;
        }
        repository.save(new SentNoticeEntity(userId, planId, at));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public long countSince(UUID userId, Instant since) {
        return repository.countSince(userId, since);
    }
}

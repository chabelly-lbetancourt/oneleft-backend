package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataTrackRecordRepository extends JpaRepository<TrackRecordEntity, TrackRecordEntity.Key> {

    List<TrackRecordEntity> findByIdPlanId(UUID planId);
}

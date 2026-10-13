package es.upm.miw.oneleft.plans.infrastructure.persistence;

import es.upm.miw.oneleft.plans.domain.model.CheckIn;
import es.upm.miw.oneleft.plans.domain.model.DeviceLocation;
import es.upm.miw.oneleft.plans.domain.port.out.CheckInRepository;
import es.upm.miw.oneleft.plans.domain.port.out.MeetingPointGeofence;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Check-ins (HU-025) in PostgreSQL, and the geofence of the meeting point with PostGIS. */
@Repository
class JpaCheckInRepository implements CheckInRepository, MeetingPointGeofence {

    private final SpringDataCheckInRepository checkIns;
    private final SpringDataPlanRepository plans;

    JpaCheckInRepository(SpringDataCheckInRepository checkIns, SpringDataPlanRepository plans) {
        this.checkIns = checkIns;
        this.plans = plans;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckIn> find(UUID planId, UUID userId) {
        return checkIns.findById(new CheckInEntity.Key(planId, userId)).map(CheckInEntity::toDomain);
    }

    @Override
    @Transactional
    public CheckIn save(CheckIn checkIn) {
        return checkIns.save(new CheckInEntity(checkIn)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckIn> findByPlan(UUID planId) {
        return checkIns.findByIdPlanIdOrderByCheckedInAt(planId).stream().map(CheckInEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contains(UUID planId, DeviceLocation location, int meters) {
        return Boolean.TRUE.equals(plans.isWithin(planId, location.latitude(), location.longitude(), meters));
    }
}

package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.NearbyPlan;
import es.upm.miw.oneleft.plans.domain.model.NearbySearch;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanLimitException;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.ReputationPerks;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanCommand;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.in.QueryPlansUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.Blocks;
import es.upm.miw.oneleft.plans.domain.port.out.PerksDirectory;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Publishing and reading plans. Each person organizes a limited number of plans at once, more with the perks of their
 * reputation (HU-063): the reputation service is only asked when they reach the limit of everyone.
 */
@Service
public class PlanService implements PublishPlanUseCase, QueryPlansUseCase {

    private final PlanRepository plans;
    private final PlanEventPublisher events;
    private final Blocks blocks;
    private final PerksDirectory perks;
    private final Clock clock;

    public PlanService(PlanRepository plans, PlanEventPublisher events, Blocks blocks, Clock clock) {
        this(plans, events, blocks, PerksDirectory.NONE, clock);
    }

    @Autowired
    public PlanService(PlanRepository plans, PlanEventPublisher events, Blocks blocks, PerksDirectory perks,
                       Clock clock) {
        this.plans = plans;
        this.events = events;
        this.blocks = blocks;
        this.perks = perks;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Plan publish(PublishPlanCommand command) {
        checkActivePlans(command.organizer().id());
        var plan = plans.save(Plan.publish(command.organizer(), command.activity(), command.activityOther(),
                command.title(), command.description(), command.meetingPoint(), command.startsAt(), command.spots(),
                command.level(), command.minParticipants(), command.minimumDeadline(), command.requiresApproval(),
                command.audience(), clock));
        events.publish(plan.publishedEvent());
        return plan;
    }

    /** The plans of the organizer that have not started and are not cancelled count; the rest do not */
    private void checkActivePlans(UUID organizerId) {
        var active = plans.findByOrganizerStartingAfter(organizerId, clock.instant()).size();
        if (active >= ReputationPerks.BASE_PLANS) {
            var max = perks.perksOf(organizerId).maxActivePlans();
            if (active >= max) {
                throw new PlanLimitException(max);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Plan plan(UUID planId) {
        return plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
    }

    @Override
    @Transactional(readOnly = true)
    public Plan plan(UUID planId, UUID viewerId) {
        var plan = plan(planId);
        var inIt = plan.isMember(viewerId) || plan.isWaiting(viewerId);
        if (!inIt && blocks.between(plan.organizer().id(), viewerId)) {
            // As if it did not exist: the block stays private
            throw new PlanNotFoundException(planId);
        }
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> upcomingPlansOrganizedBy(UUID organizerId) {
        return plans.findByOrganizerStartingAfter(organizerId, clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyPlan> nearbyPlans(NearbySearch search) {
        return plans.findOpenNearby(search, clock.instant(), NearbySearch.MAX_RESULTS).stream()
                .map(plan -> new NearbyPlan(plan, search.distanceTo(plan.meetingPoint().latitude(),
                        plan.meetingPoint().longitude())))
                .toList();
    }
}

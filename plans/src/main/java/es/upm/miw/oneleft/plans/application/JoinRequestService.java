package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.JoinRequested;
import es.upm.miw.oneleft.plans.domain.model.NotOrganizerException;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanNotFoundException;
import es.upm.miw.oneleft.plans.domain.model.RequestDecided;
import es.upm.miw.oneleft.plans.domain.model.RequestDecision;
import es.upm.miw.oneleft.plans.domain.model.RequestStatus;
import es.upm.miw.oneleft.plans.domain.port.in.ManageJoinRequestsUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.AvailabilityRepository;
import es.upm.miw.oneleft.plans.domain.port.out.Blocks;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Requests for a spot and the organizer's decisions (HU-069), with optimistic locking like joining (see
 * {@link OptimisticPlanUpdates}): more people than free spots can never be accepted. Events are published after the
 * change is saved; the person accepted is no longer free (HU-035) and the organizer hears of the join as always.
 */
@Service
public class JoinRequestService implements ManageJoinRequestsUseCase {

    private final OptimisticPlanUpdates updates;
    private final PlanRepository plans;
    private final PlanEventPublisher events;
    private final AvailabilityRepository availabilities;
    private final Blocks blocks;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public JoinRequestService(OptimisticPlanUpdates updates, PlanRepository plans, PlanEventPublisher events,
                              AvailabilityRepository availabilities, Blocks blocks, Clock clock) {
        this.updates = updates;
        this.plans = plans;
        this.events = events;
        this.availabilities = availabilities;
        this.blocks = blocks;
        this.clock = clock;
    }

    /** For someone blocked by the organizer, or who blocked them, the plan does not exist (HU-033). */
    @Override
    public Plan request(UUID planId, UUID userId, String name, String message) {
        var plan = updates.update(planId, current -> {
            if (blocks.between(current.organizer().id(), userId)) {
                throw new PlanNotFoundException(planId);
            }
            return current.request(userId, name, message, clock);
        });
        events.publish(new JoinRequested(planId, plan.organizer().id(), plan.title(), userId, name, clock.instant()));
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public Plan requestsOf(UUID planId, UUID organizerId) {
        var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        if (!plan.organizer().id().equals(organizerId)) {
            throw new NotOrganizerException();
        }
        return plan;
    }

    @Override
    public RequestDecision accept(UUID planId, UUID organizerId, UUID requesterId) {
        var decision = updates.update(planId, plan -> plan.accept(organizerId, requesterId, clock),
                RequestDecision::plan);
        availabilities.delete(requesterId);
        var plan = decision.plan();
        events.publish(plan.joinedEvent());
        tell(plan, decision.request().userId(), RequestStatus.ACCEPTED, false);
        decision.autoRejected().forEach(rejected -> tell(plan, rejected.userId(), RequestStatus.REJECTED, true));
        return decision;
    }

    @Override
    public RequestDecision reject(UUID planId, UUID organizerId, UUID requesterId) {
        var decision = updates.update(planId, plan -> plan.reject(organizerId, requesterId, clock),
                RequestDecision::plan);
        tell(decision.plan(), requesterId, RequestStatus.REJECTED, false);
        return decision;
    }

    @Override
    public Plan withdraw(UUID planId, UUID userId) {
        return updates.update(planId, plan -> plan.withdraw(userId, clock));
    }

    private void tell(Plan plan, UUID requesterId, RequestStatus decision, boolean planFull) {
        events.publish(new RequestDecided(plan.id(), plan.title(), requesterId, decision, planFull, clock.instant()));
    }
}

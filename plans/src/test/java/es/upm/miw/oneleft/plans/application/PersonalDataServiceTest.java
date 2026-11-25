package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MemoryFormat;
import es.upm.miw.oneleft.plans.domain.model.MemoryKey;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.model.PlanCancelled;
import es.upm.miw.oneleft.plans.domain.model.PlanStatus;
import es.upm.miw.oneleft.plans.domain.model.StoredMemory;
import es.upm.miw.oneleft.plans.domain.port.out.PersonalData;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.padelPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * HU-045: the erasure of an account cancels the plans they organized that have not started, telling everyone in them,
 * and with the photos of HU-052 also deletes them from the storage.
 */
class PersonalDataServiceTest {

    private static final StoredMemory PHOTO = new StoredMemory(1000, "image/webp", MemoryFormat.WEBP);

    private final UUID user = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID plan = UUID.randomUUID();
    private final List<UUID> erased = new ArrayList<>();
    /** How many cancellations had been published when the data of each erasure was removed. */
    private final List<Integer> publishedBeforeErasing = new ArrayList<>();
    private final MemoryServiceTest.InMemoryStorage storage = new MemoryServiceTest.InMemoryStorage();
    private final JoinPlanServiceTest.Plans plans = new JoinPlanServiceTest.Plans();
    private final PlanEventPublisher publisher = mock(PlanEventPublisher.class);
    private final PersonalDataService service = new PersonalDataService(new PersonalData() {
        @Override
        public Map<String, List<Map<String, Object>>> of(UUID userId) {
            return Map.of("memories", List.of(Map.of("user", userId)));
        }

        @Override
        public void erase(UUID userId) {
            erased.add(userId);
            publishedBeforeErasing.add(mockingDetails(publisher).getInvocations().size());
        }
    }, storage, new PlanCancellationService(plans, new OptimisticPlanUpdates(plans,
            TransactionOperations.withoutTransaction()), publisher, CLOCK), TransactionOperations.withoutTransaction());

    private Plan planOf(UUID organizer, Duration startsIn) {
        return plans.save(padelPlan(new Organizer(organizer, "Ana"), startsIn, CLOCK));
    }

    private List<PlanCancelled> published(int times) {
        var events = ArgumentCaptor.forClass(PlanCancelled.class);
        verify(publisher, times(times)).publish(events.capture());
        return events.getAllValues();
    }

    @Test
    void erasingTheAccountOfAnOrganizerCancelsEachPlanNotStartedAndTellsWhoeverWasInIt() {
        var lucia = UUID.randomUUID();
        var diego = UUID.randomUUID();
        var soon = plans.save(planOf(user, Duration.ofMinutes(30)).join(lucia, "Lucía", CLOCK)
                .joinWaitlist(diego, "Diego", CLOCK));
        var later = planOf(user, Duration.ofHours(3));
        var ofSomeoneElse = planOf(other, Duration.ofHours(1));

        service.erase(user);
        // The event can arrive twice
        service.erase(user);

        assertThat(List.of(soon, later)).allSatisfy(plan -> {
            assertThat(plans.data.get(plan.id()).status()).isEqualTo(PlanStatus.CANCELLED);
            assertThat(plans.data.get(plan.id()).waitlist()).isEmpty();
        });
        assertThat(plans.data.get(ofSomeoneElse.id()).status()).isEqualTo(PlanStatus.OPEN);
        var events = published(2);
        assertThat(events).extracting(PlanCancelled::planId).containsExactly(soon.id(), later.id());
        assertThat(events.get(0).recipientIds()).containsExactly(lucia, diego);
        assertThat(events.get(1).recipientIds()).isEmpty();
        // Not ORGANIZER: less than an hour before the start, that reason would cost points to an account that is gone
        assertThat(events).extracting(PlanCancelled::reason).containsOnly(PlanCancelled.Reason.ACCOUNT_DELETED);
        // Everyone was read and told before the data of the person was erased
        assertThat(publishedBeforeErasing).containsExactly(2, 2);
    }

    @Test
    void ifAPlanCannotBeCancelledNothingIsErasedSoThatTheEventIsRetried() {
        planOf(user, Duration.ofHours(1));
        plans.conflictsLeft = OptimisticPlanUpdates.MAX_ATTEMPTS;

        assertThatThrownBy(() -> service.erase(user)).isInstanceOf(JoinRejectedException.class);

        assertThat(erased).isEmpty();
        verifyNoInteractions(publisher);
    }

    @Test
    void erasingAnAccountDeletesTheirPhotosOfEveryAlbumAndNobodyElses() {
        var mine = MemoryKey.newFor(user, plan, MemoryFormat.WEBP);
        var inAnotherPlan = MemoryKey.newFor(user, UUID.randomUUID(), MemoryFormat.WEBP);
        var notMine = MemoryKey.newFor(other, plan, MemoryFormat.WEBP);
        List.of(mine, inAnotherPlan, notMine).forEach(key -> storage.upload(key, PHOTO, Instant.EPOCH));

        service.erase(user);
        // The event can arrive twice
        service.erase(user);

        assertThat(erased).containsExactly(user, user);
        assertThat(storage.objects).containsOnlyKeys(notMine);
    }

    @Test
    void theRestIsErasedEvenIfTheStorageDoesNotAnswer() {
        storage.down = true;

        service.erase(user);

        assertThat(erased).containsExactly(user);
    }

    @Test
    void theExportIsWhatTheStorageOfThisServiceKeeps() {
        assertThat(service.export(user)).containsOnlyKeys("memories");
    }
}

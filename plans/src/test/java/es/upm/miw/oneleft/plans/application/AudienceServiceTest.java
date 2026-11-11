package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.application.JoinPlanServiceTest.Plans;
import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Audience;
import es.upm.miw.oneleft.plans.domain.model.AudienceGender;
import es.upm.miw.oneleft.plans.domain.model.AudienceMember;
import es.upm.miw.oneleft.plans.domain.model.AudienceUnavailableException;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.PersonGender;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.out.PlanEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static es.upm.miw.oneleft.plans.PlanFixtures.CLOCK;
import static es.upm.miw.oneleft.plans.PlanFixtures.NOW;
import static es.upm.miw.oneleft.plans.PlanFixtures.ana;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** HU-070: who fits a plan is decided in the backend, with gender and age from the users service. */
class AudienceServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();
    private final Map<UUID, AudienceMember> people = new HashMap<>();
    private boolean usersDown;
    private int asked;
    private final AudienceService service = new AudienceService(userId -> {
        asked++;
        if (usersDown) {
            throw new AudienceUnavailableException("down", null);
        }
        return Optional.ofNullable(people.get(userId));
    });

    private static Plan plan(Audience audience) {
        return Plan.publish(ana(), Activity.PADEL, null, "Pádel", null, new MeetingPoint("Courts", 40.39, -3.62),
                NOW.plus(Duration.ofHours(1)), 2, null, null, null, false, audience, CLOCK);
    }

    private static void rejected(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(JoinRejectedException.class)
                .satisfies(error -> assertThat(((JoinRejectedException) error).code()).isEqualTo(code));
    }

    @Test
    void aPlanForEveryoneNeedsNothingFromTheUsersService() {
        service.check(plan(Audience.EVERYONE), LUCIA);

        assertThat(asked).isZero();
    }

    @Test
    void whoFitsJoinsAndWhoDoesNotIsRejectedWithItsOwnCode() {
        var women25to40 = plan(new Audience(AudienceGender.WOMAN, 25, 40));
        people.put(LUCIA, new AudienceMember(PersonGender.WOMAN, 33));
        people.put(DIEGO, new AudienceMember(PersonGender.MAN, 33));

        service.check(women25to40, LUCIA);
        rejected(() -> service.check(women25to40, DIEGO), "plan.audienceMismatch");
        people.put(LUCIA, new AudienceMember(PersonGender.WOMAN, 50));
        rejected(() -> service.check(women25to40, LUCIA), "plan.audienceMismatch");
    }

    @Test
    void withoutTheirDataOrIfTheyCannotBeKnownThePersonDoesNotJoin() {
        var men = plan(new Audience(AudienceGender.MAN, null, null));

        rejected(() -> service.check(men, LUCIA), "demographics.required");
        usersDown = true;
        rejected(() -> service.check(men, LUCIA), "demographics.unavailable");
        assertThat(service.viewer(LUCIA)).as("a search just shows the plans for everyone").isEmpty();
    }

    @Test
    void theJoinServicesRefuseWhoDoesNotFitAndChangeNothing() {
        var plans = new Plans();
        var saved = plans.save(plan(new Audience(AudienceGender.WOMAN, null, null)));
        people.put(DIEGO, new AudienceMember(PersonGender.MAN, 30));
        var join = new JoinPlanService(new OptimisticPlanUpdates(plans, TransactionOperations.withoutTransaction()),
                mock(PlanEventPublisher.class), new Availabilities(), new BlockedPairs(), service, CLOCK);
        var waitlist = new ParticipationService(new OptimisticPlanUpdates(plans,
                TransactionOperations.withoutTransaction()), mock(PlanEventPublisher.class), new Availabilities(),
                service, CLOCK);

        assertThatThrownBy(() -> join.join(saved.id(), DIEGO, "Diego")).isInstanceOf(JoinRejectedException.class);
        assertThatThrownBy(() -> waitlist.joinWaitlist(saved.id(), DIEGO, "Diego"))
                .isInstanceOf(JoinRejectedException.class);
        people.put(LUCIA, new AudienceMember(PersonGender.WOMAN, 30));
        assertThat(join.join(saved.id(), LUCIA, "Lucía").participants()).hasSize(1);
        assertThat(plans.data.get(saved.id()).participants()).hasSize(1);
    }
}

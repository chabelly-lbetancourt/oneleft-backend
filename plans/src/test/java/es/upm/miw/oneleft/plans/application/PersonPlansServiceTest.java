package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.PersonActivityException;
import es.upm.miw.oneleft.plans.domain.model.RecentPlan;
import es.upm.miw.oneleft.plans.domain.port.in.PersonPlansUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.ActivityAccess;
import es.upm.miw.oneleft.plans.domain.port.out.PlanRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PersonPlansServiceTest {

    private final PlanRepository plans = mock(PlanRepository.class);
    private final ActivityAccess access = mock(ActivityAccess.class);
    private final PersonPlansService service = new PersonPlansService(plans, access);
    private final UUID ana = UUID.randomUUID();
    private final UUID lucia = UUID.randomUUID();
    private final List<RecentPlan> recent = List.of(new RecentPlan(UUID.randomUUID(), "Rodaje suave",
            Activity.RUNNING, Instant.parse("2026-10-14T10:00:00Z"), true));

    @Test
    void myOwnPlansNeedNoQuestionToTheSocialService() {
        when(plans.findRecentlyHeldWith(ana, PersonPlansUseCase.RECENT_PLANS)).thenReturn(recent);

        assertThat(service.recentPlansOf(ana, ana)).isEqualTo(recent);
        verifyNoInteractions(access);
    }

    @Test
    void thePlansOfSomeoneElseAreSeenOnlyIfTheSocialServiceSaysSo() {
        when(plans.findRecentlyHeldWith(any(), any(int.class))).thenReturn(recent);
        when(access.visible(ana)).thenReturn(true);
        assertThat(service.recentPlansOf(lucia, ana)).isEqualTo(recent);

        when(access.visible(ana)).thenReturn(false);
        assertThatThrownBy(() -> service.recentPlansOf(lucia, ana))
                .isInstanceOfSatisfying(PersonActivityException.class,
                        error -> assertThat(error.code()).isEqualTo("social.private"));
    }

    @Test
    void whenItCannotBeKnownNothingIsShown() {
        when(access.visible(ana)).thenThrow(new PersonActivityException(
                PersonActivityException.Reason.UNAVAILABLE, "down"));

        assertThatThrownBy(() -> service.recentPlansOf(lucia, ana))
                .isInstanceOfSatisfying(PersonActivityException.class,
                        error -> assertThat(error.code()).isEqualTo("social.unavailable"));
        verifyNoInteractions(plans);
    }
}

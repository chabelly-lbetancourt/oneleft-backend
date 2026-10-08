package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.AlertLimitException;
import es.upm.miw.oneleft.notifications.domain.model.AlertNotFoundException;
import es.upm.miw.oneleft.notifications.domain.model.SavedAlert;
import es.upm.miw.oneleft.notifications.domain.port.in.ManageAlertsUseCase;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-036: each person manages their own alerts, up to the limit. */
class AlertServiceTest {

    private static final UUID LUCIA = UUID.randomUUID();
    private static final UUID DIEGO = UUID.randomUUID();

    private final NearbyPlanNotifierTest.Alerts alerts = new NearbyPlanNotifierTest.Alerts();
    private final AlertService service = new AlertService(alerts);

    private static SavedAlert data(String name) {
        // The id and the owner of the data are ignored
        return new SavedAlert(UUID.randomUUID(), DIEGO, name, Set.of(), null, 40.39, -3.63, 2_000, Set.of(), null,
                null);
    }

    @Test
    void createsAlertsForTheirOwnerUpToTheLimit() {
        for (var index = 0; index < ManageAlertsUseCase.MAX_ALERTS; index++) {
            var created = service.create(LUCIA, data("Alerta " + index));
            assertThat(created.userId()).isEqualTo(LUCIA);
        }

        assertThatThrownBy(() -> service.create(LUCIA, data("Una más")))
                .isInstanceOfSatisfying(AlertLimitException.class,
                        error -> assertThat(error.code()).isEqualTo("alerts.limit"));
        assertThat(service.alertsOf(LUCIA)).hasSize(ManageAlertsUseCase.MAX_ALERTS);
        assertThat(service.alertsOf(DIEGO)).isEmpty();
    }

    @Test
    void onlyTheOwnerEditsOrDeletesAnAlert() {
        var alert = service.create(LUCIA, data("Pádel"));

        assertThat(service.update(LUCIA, alert.id(), data("Pádel al salir")).name()).isEqualTo("Pádel al salir");
        assertThatThrownBy(() -> service.update(DIEGO, alert.id(), data("Mía")))
                .isInstanceOfSatisfying(AlertNotFoundException.class,
                        error -> assertThat(error.code()).isEqualTo("alerts.notFound"));
        assertThatThrownBy(() -> service.delete(DIEGO, alert.id())).isInstanceOf(AlertNotFoundException.class);

        service.delete(LUCIA, alert.id());
        assertThat(service.alertsOf(LUCIA)).isEmpty();
        var unknown = UUID.randomUUID();
        assertThatThrownBy(() -> service.delete(LUCIA, unknown)).isInstanceOf(AlertNotFoundException.class);
    }
}

package es.upm.miw.oneleft.plans.infrastructure.seed;

import es.upm.miw.oneleft.plans.domain.model.Activity;
import es.upm.miw.oneleft.plans.domain.model.Level;
import es.upm.miw.oneleft.plans.domain.model.MeetingPoint;
import es.upm.miw.oneleft.plans.domain.model.Organizer;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanCommand;
import es.upm.miw.oneleft.plans.domain.port.in.PublishPlanUseCase;
import es.upm.miw.oneleft.plans.domain.port.in.QueryPlansUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Demo plans around Vallecas (Madrid) for the dev and pre environments. Plans only live for a few hours, so start
 * times are relative to the start of the service: if the demo plans have already started, a new set is published.
 * They go through the publish use case, so they follow the same rules as the API and emit the real-time event.
 */
@Component
@Profile(DemoPlansSeeder.PROFILE)
public class DemoPlansSeeder implements ApplicationRunner {

    /** Explicit opt-in, and never together with production. */
    public static final String PROFILE = "seed & !pro";

    /** Test users of the development realm (fixed ids in oneleft-realm.json). */
    public static final Organizer ANA = new Organizer(UUID.fromString("a624d063-bd1e-442d-b52d-8de2df356c13"), "Ana");
    public static final Organizer LUCIA = new Organizer(UUID.fromString("5d1c7c2e-2f0b-4d6e-9a43-0c1e7a9b0001"), "Lucía");
    static final Organizer DIEGO = new Organizer(UUID.fromString("5d1c7c2e-2f0b-4d6e-9a43-0c1e7a9b0002"), "Diego");
    static final Organizer MARTA = new Organizer(UUID.fromString("5d1c7c2e-2f0b-4d6e-9a43-0c1e7a9b0003"), "Marta");
    static final Organizer JAVIER = new Organizer(UUID.fromString("5d1c7c2e-2f0b-4d6e-9a43-0c1e7a9b0004"), "Javier");

    private static final Logger log = LoggerFactory.getLogger(DemoPlansSeeder.class);

    private record DemoPlan(Organizer organizer, Activity activity, String title, String description,
                            MeetingPoint place, Duration startsIn, int spots, Level level) {
    }

    static final List<DemoPlan> PLANS = List.of(
            new DemoPlan(LUCIA, Activity.PADEL, "Partido de pádel, falta uno", "Nivel medio, pista cubierta",
                    new MeetingPoint("Pistas del polideportivo de Vallecas", 40.3912, -3.6287), Duration.ofMinutes(90),
                    1, Level.INTERMEDIATE),
            new DemoPlan(ANA, Activity.RUNNING, "Rodaje suave por el parque", "5 km a ritmo tranquilo",
                    new MeetingPoint("Parque Lineal de Palomeras", 40.3845, -3.6352), Duration.ofMinutes(45), 3,
                    Level.BEGINNER),
            new DemoPlan(MARTA, Activity.BOARD_GAMES, "Catan en la cafetería", "Traemos el juego y la ampliación",
                    new MeetingPoint("Café La Partida", 40.3968, -3.6221), Duration.ofMinutes(150), 2, null),
            new DemoPlan(DIEGO, Activity.FOOTBALL, "Pachanga 7 contra 7", "Faltan cuatro para completar",
                    new MeetingPoint("Campo de fútbol de Entrevías", 40.3801, -3.6690), Duration.ofHours(3), 4, null),
            new DemoPlan(JAVIER, Activity.TENNIS, "Dobles de tenis", "Buscamos pareja de nivel alto",
                    new MeetingPoint("Club de tenis de Moratalaz", 40.4070, -3.6440), Duration.ofHours(4), 2,
                    Level.ADVANCED),
            new DemoPlan(DIEGO, Activity.CYCLING, "Ruta en bici por el Anillo Verde", "Unos 25 km, sin prisa",
                    new MeetingPoint("Entrada del Parque de la Gavia", 40.3730, -3.6100), Duration.ofHours(5), 3,
                    Level.INTERMEDIATE),
            new DemoPlan(LUCIA, Activity.CINEMA, "Sesión doble de cine", null,
                    new MeetingPoint("Cines del centro", 40.4169, -3.7035), Duration.ofHours(8), 2, null),
            new DemoPlan(MARTA, Activity.CONCERTS, "Me sobra una entrada para un concierto",
                    "Concierto de indie en sala pequeña",
                    new MeetingPoint("Sala de conciertos de Malasaña", 40.4260, -3.7030), Duration.ofHours(10), 1,
                    null));

    private final PublishPlanUseCase publishPlan;
    private final QueryPlansUseCase queryPlans;
    private final Clock clock;

    public DemoPlansSeeder(PublishPlanUseCase publishPlan, QueryPlansUseCase queryPlans, Clock clock) {
        this.publishPlan = publishPlan;
        this.queryPlans = queryPlans;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!queryPlans.upcomingPlansOrganizedBy(LUCIA.id()).isEmpty()) {
            log.info("Seed: the demo plans are still upcoming, nothing to do");
            return;
        }
        var now = clock.instant();
        PLANS.forEach(plan -> publishPlan.publish(new PublishPlanCommand(plan.organizer(), plan.activity(),
                plan.title(), plan.description(), plan.place(), now.plus(plan.startsIn()), plan.spots(),
                plan.level())));
        log.info("Seed: {} demo plans published around Vallecas", PLANS.size());
    }
}

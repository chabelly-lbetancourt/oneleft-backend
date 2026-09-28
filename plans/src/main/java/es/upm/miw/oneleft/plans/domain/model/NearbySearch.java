package es.upm.miw.oneleft.plans.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Búsqueda de planes abiertos cerca de una posición (HU-004): dentro de un radio, de ciertas actividades y que
 * empiecen pronto. Quien busca no ve sus propios planes.
 *
 * @param activities actividades que interesan; vacío significa todas
 * @param requesterId quien busca, o {@code null} si no se conoce
 */
public record NearbySearch(double latitude, double longitude, int radiusMeters, Set<Activity> activities,
                           Duration startsWithin, UUID requesterId) {

    public static final int MIN_RADIUS_METERS = 500;
    public static final int MAX_RADIUS_METERS = 25_000;
    public static final int DEFAULT_RADIUS_METERS = 5_000;
    public static final Duration MIN_STARTS_WITHIN = Duration.ofHours(1);
    /** Resultados como máximo: suficientes para una pantalla de móvil y un mapa legible. */
    public static final int MAX_RESULTS = 50;

    public NearbySearch {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenadas de búsqueda fuera de rango");
        }
        if (radiusMeters < MIN_RADIUS_METERS || radiusMeters > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException("El radio debe estar entre " + MIN_RADIUS_METERS + " m y "
                    + MAX_RADIUS_METERS / 1000 + " km");
        }
        if (startsWithin == null) {
            startsWithin = Plan.MAX_HORIZON;
        }
        if (startsWithin.compareTo(MIN_STARTS_WITHIN) < 0 || startsWithin.compareTo(Plan.MAX_HORIZON) > 0) {
            throw new IllegalArgumentException("Solo se pueden buscar planes que empiecen en las próximas "
                    + MIN_STARTS_WITHIN.toHours() + " a " + Plan.MAX_HORIZON.toHours() + " horas");
        }
        activities = activities == null ? Set.of() : Set.copyOf(activities);
    }

    public double distanceTo(double otherLatitude, double otherLongitude) {
        return GeoDistance.meters(latitude, longitude, otherLatitude, otherLongitude);
    }

    public boolean includes(Activity activity) {
        return activities.isEmpty() || activities.contains(activity);
    }

    /** Fin de la ventana de búsqueda: los planes deben empezar entre {@code now} y este instante. */
    public Instant until(Instant now) {
        return now.plus(startsWithin);
    }

    /**
     * Decide si un plan recién publicado aparece en esta búsqueda. Aplica los mismos criterios que la consulta a la
     * base de datos, para que la lista en tiempo real y la recarga coincidan.
     */
    public boolean matches(PlanPublished event, Instant now) {
        return includes(event.activity())
                && !event.organizerId().equals(requesterId)
                && event.freeSpots() > 0
                && event.startsAt().isAfter(now)
                && !event.startsAt().isAfter(until(now))
                && distanceTo(event.latitude(), event.longitude()) <= radiusMeters;
    }
}

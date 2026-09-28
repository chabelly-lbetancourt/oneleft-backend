package es.upm.miw.oneleft.plans.domain.model;

/** Plan encontrado en una búsqueda cercana, con la distancia desde la posición de quien busca. */
public record NearbyPlan(Plan plan, double distanceMeters) {
}

package es.upm.miw.oneleft.plans.domain.model;

/** A place suggested for the meeting point, with its distance in metres to whoever is typing (HU-071). */
public record PlaceSuggestion(Place place, int distanceMeters) {
}

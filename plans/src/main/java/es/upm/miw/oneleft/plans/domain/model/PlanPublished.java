package es.upm.miw.oneleft.plans.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio: se ha publicado un plan. Lo consumen los servicios de búsqueda por proximidad y de
 * notificaciones para avisar a las personas cercanas.
 */
public record PlanPublished(UUID planId, UUID organizerId, Activity activity, double latitude, double longitude,
                            Instant startsAt, int freeSpots, Level level, Instant occurredAt) {
}

package es.upm.miw.oneleft.social.domain.model;

import java.util.UUID;

/** Whoever is using the service, with the name of their account. */
public record Caller(UUID userId, String name) {
}

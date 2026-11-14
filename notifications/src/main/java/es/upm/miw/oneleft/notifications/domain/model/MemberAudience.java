package es.upm.miw.oneleft.notifications.domain.model;

import java.util.UUID;

/**
 * What this service knows of a person to decide whether a plan is for them (HU-070): their gender
 * ({@code WOMAN}, {@code MAN}, {@code NON_BINARY} or {@code PREFER_NOT_TO_SAY}) and their age in years, as the users
 * service announces them. The date of birth never reaches this service.
 */
public record MemberAudience(UUID userId, String gender, int age) {
}

package es.upm.miw.oneleft.users.domain.port.in;

/** Keeping the other services up to date with the age of people, which changes on their birthday (HU-070). */
public interface AnnounceAgesUseCase {

    /** Announces the new age of everyone who has their birthday today; returns how many were announced. */
    int announceBirthdays();
}
